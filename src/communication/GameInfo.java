package communication;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import adt.IAction;
import adt.IActionTriple;
import adt.IActionDouble;
import adt.ICloneStringable;
import audio.AudioRemote;
import audio.SfxTypes;
import comNew.LocalRemote2;
import comNew.Remote2;
import comNew.SteamRemote2;
import comNew.TestRemote;
import communication.remote.Message;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.modal.UIBonusModal;
import game_modes.*;
import main.Features;
import main.Main;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import player_local.car.Car;
import player_local.upgrades.Store;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeResult;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.game.GameRemoteMaster;
import scenes.game.PlayerLobbyInfo;
import scenes.game.Race;
import scenes.regular.ReplayVisual;

/**
 * Holds info about who is a part of this game. Also holds info about the cars
 * when racing.
 */

public class GameInfo implements ICloneStringable {

    public static final int JOIN_TYPE_VIA_CLIENT_NEW_HOST = 0, JOIN_TYPE_VIA_CREATOR = 1, JOIN_TYPE_VIA_CLIENT = 2;

    // MYPLAYER
    public Player player; // current - can be switched out
    public final Store store = new Store();
    public boolean resigned;
    private Player truePlayer;

    // BONUS
    public static UIBonusModal bonusModal;

    private final Map<Byte, Player> players = new ConcurrentHashMap<>();
    private final Map<Long, Player> lostPlayers = new HashMap<>();
    private final CopyOnWriteArrayList<Player> sortedPlayers = new CopyOnWriteArrayList<>();
    private long[] raceLights;
    private String raceLobbyString;
    private IActionDouble<List<PlayerLobbyInfo>, String> actionLobbyString;
    private Consumer<String> actionNewChat;

    public long gmCreationTime, newEndGoalTime;
    private GameMode gm;

    public int overrideRound = -1;

    public boolean raceLobbyStringFinalized;
    public boolean countdownPaused;
    private long countdown;
    private int gameID;
    private short paidAtRound = 0, finishedAtRound = -1;
    private int gamemodeScrollingIndex;
    private IActionTriple<Car, Boolean, Integer> actionFinishPlayerAnimation;
    private Consumer<GameMode> actionGameModeUpdated;
    private IAction actionRemakeUpgradeView;
    private IAction actionEveryoneFinished;
    private Remote2 remote;
    private final GameRemoteMaster game;
    private final GameType type;

	public long raceSeed;

    public static boolean exists;

    public GameInfo(GameRemoteMaster game, GameType type, String ip) {
        exists = true;
        this.type = type;
        this.game = game;
        raceSeed = System.currentTimeMillis();
        store.setBonusModal(bonusModal);

        Upgrade.placedNeighbourChangeLVL = 3;

        if (game != null)
            game.init();

        clearRaceCountdown();

        Layer.minTimesMod = 0.1f;
        if (Main.DEBUG)
            Tool.improvementPointsNeeded = 0;
        else
            Tool.improvementPointsNeeded = 8;

        try {
            if (type != GameType.NONE && !type.isSinglePlayer()) {
                remote = new Remote2(this,
                        switch (type) {
                            case CREATING_ONLINE, JOINING_ONLINE -> new SteamRemote2(this);
                            case CREATING_LAN, JOINING_LAN -> new LocalRemote2(ip, type);
                            case DIRECT -> new TestRemote();
                            default -> null;
                        });
            }
        } catch (IOException e) {
            remote = null;
            return;
        }

        if (type.isCreating()) {
            gameID = Math.abs(Features.ran.nextInt()) + 1;
//            System.out.println("GameInfo - gameID: " + gameID);
            if (type.isSinglePlayer()) {
                gamemodeScrollingIndex = 0;
            } else {
                gamemodeScrollingIndex = GameModes.getSingleplayerModesCount();
            }
        } else {
            gameID = -1;
        }

    }
    
	public void reset() {
        ReplayVisual.saveReplay(this);
        overrideRound = 0;
        paidAtRound = 0;
        finishedAtRound = -1;
        countdownPaused = true;
        init(null, 0, null, null);
        updateRaceLights();	
        
        for (var p : getPlayersIncludingLostOnes()) {
        	var name = p.name;
        	var id = p.id;
        	var sid = p.steamID;
        	var role = p.role;
        	Translator.setCloneString(p, new Player());
        	p.name = name;
        	p.id = id;
        	p.steamID = sid;
        	p.role = p.wasHost ? Player.HOST : Player.PLAYER;
        }
    }

    public GameInfo(GameRemoteMaster game, GameType type) {
        this(game, type, "localhost");
    }

    /**
     * Test only!
     */
    public GameInfo() {
        this(null, GameType.NONE);
    }

    private byte generateID() {
        byte highestExistingId = 0;
        for (var player : players.values()) {
            if (player.id > highestExistingId)
                highestExistingId = player.id;
        }

        return ++highestExistingId;
    }

    public void addHost(long steamID) {
        join(new Player("Host", Player.DEFAULT_ID, Player.HOST, steamID), GameInfo.JOIN_TYPE_VIA_CLIENT_NEW_HOST, null,
                0, 0);
    }

    public Player getHost() {
        for (var player : players.values()) {
            if (player.isHost())
                return player;
        }
        return null;
    }

    /**
     * @param afterJoined and host not used
     */
    public Player join(Player player, int typeJoin, Consumer<Player> afterJoined, long hostID, int arg) {
        if (type != GameType.NONE && !type.isSinglePlayer() && (remote == null || !remote.way.isOpen())) {
            game.leaveGame = true;
            System.out.println("Reason for ending: Tried to join a closed lobby");
            return null;
        }
        boolean jump = false;

        // On a different version
        long steamID = player.steamID;
        if (steamID != 0 && isGameStarted()) {

            // Have key?
            if (lostPlayers.containsKey(steamID)) {
                player = lostPlayers.remove(steamID);
                player.rejoinResetHistoryIndex();

                players.put(player.id, player);

                if (this.player.isHost() && lostPlayers.isEmpty() && remote.way instanceof LocalRemote2 lanRemote) {
                    lanRemote.closeUDP();
                }
                jump = true;
            }

            // Joined, but is still in? Perhaps lost connection and connected before
            // server
            // noticed?
            if (!jump) {
                for (Player other : players.values()) {
                    // same discID or SteamID?
                    if (steamID == other.steamID) {
                        other.name = player.name;
                        other.role = player.role;
                        player = other;
                        player.rejoinResetHistoryIndex();

                        jump = true;
                        break;
                    }
                }
            }
        }

        if (!jump) {
            // new player
            if (player.isHost())
                player.id = 0;
            else if (player.id == Player.DEFAULT_ID) {
                player.id = generateID();
                if (!isSteam()) {
                    boolean foundSameId;
                    do {
                        foundSameId = false;
                        for (var other : players.values())
                            if (player.steamID == other.steamID) {
                                player.steamID = Features.generateLanId(false);
                                foundSameId = true;
                                break;
                            }
                    } while (foundSameId);
                }
            }

            players.put(player.id, player);
        }

        // successfully joined.

        if (this.player == null) { // Den som joiner er meg.
            this.player = player;
            this.truePlayer = player;
            if (remote != null) {
                remote.run();
                if (!player.isHost()) {
                    addHost(hostID);
                    remote.info.afterJoined = afterJoined;
                    remote.push(Message.msgJoin(hostID, player));
                }
            }
            if (player.isHost()) {
                // First time inits
                init(null, arg, null, null);
                updateRaceLights();
                player.joined = true;
            }
        } else if (gm != null && !gm.isGameBegun() && this.player.isHost()) {
            // Make sure that all players start with the same board - equal chances.
            newEndGoalTime++;
            gm.newEndGoal();
        }

        for (var p : players.values()) {
            p.ready = 0;
            p.readyTime = 0;
        }

        if (typeJoin == JOIN_TYPE_VIA_CREATOR) {
            // Bare host kommer hit.
            player.gameID = this.gameID;
            assert gm != null;

            if (player.isHost()) {
                store.resetTowardsPlayer(player); // Creates tiles after prices has been set
            } else if (!gm.isGameBegun()) {
                // Make sure that all players start with the same board - equal chances.
                gm.setPrices(player.upgrades);
                player.resetHistory(gm.getRound());
                if (!isCoop())
                    Translator.setCloneString(player.layer, getHost().layer);
                else
                    player.car.switchTo(getHost().getCarNameID(), getHost().getCarRep().isRandom());
            }
        }

        updateSortedPlayers();

        countdownPaused = !lostPlayers.isEmpty();

        return player;
    }

    public Player join(Player player) {
        return join(player,
                (this.player == null || this.player.isHost()
                        ? GameInfo.JOIN_TYPE_VIA_CREATOR
                        : GameInfo.JOIN_TYPE_VIA_CLIENT),
                null, 0, 0);
    }

    public void carSelectUpdate(Player player, int selectedCarIndex, boolean random, boolean updateCoop) {
        if (updateCoop && isCoop()) {
            if (player.isHost()) {
                for (var p : getPlayers()) {
                    p.carSelectionTime = player.carSelectionTime;
                    if (p.equals(player))
                        continue;
                    carSelectUpdate(p, selectedCarIndex, false, random, false);
                }
            } else {
                var host = getHost();
                carSelectUpdate(player, host.getCarNameID(), false, random, false);
                Translator.setCloneString(player.getCarRep(), host.getCarRep());
                return;
            }
        }

        player.car.switchTo(selectedCarIndex, random);

        player.resetHistory(gm.getRound());
        player.upgrades.resetTowardsCar(player.getCarRep(), player.layer);
        player.bank.reset();
        gm.setPrices(player.upgrades);

        updateLobbyString();
    }

    public void carSelectUpdate(Player player, int selectedCarIndex, boolean send, boolean random, boolean updateCoop) {
        carSelectUpdate(player, selectedCarIndex, random, updateCoop);

        if (player.equals(this.player)) {

            if (gm.getGameModeEnum().equals(GameModes.SINGLEPLAYER_CHALLENGES)) {
                ((SingleplayerChallengesMode) gm).giveStarterPoints();
                gm.updateInfo();
                gm.newEndGoal();
            } else if (gm.getGameModeEnum().equals(GameModes.CLASSIC)) {
                gm.updateInfo();
                gm.setPrices(player.upgrades);
            }

            player.carSelectionTime++;
            store.resetTowardsPlayer(player);
            if (send && remote != null)
                remote.push(Message.msgSelectCar(player, player.carSelectionTime, selectedCarIndex, random));
        }
    }

    private Player replacement() {
        Player replacement = null;
        int minimumID = Integer.MAX_VALUE;
        for (var playerWhoStayed : players.values()) {
            if (playerWhoStayed == null || playerWhoStayed.resigned)
                continue;

            if (playerWhoStayed.id < minimumID) {
                replacement = playerWhoStayed;
                minimumID = replacement.id;
            }
        }
        return replacement;
    }

    public synchronized void leave(Player player, boolean send, boolean kick) {
        System.out.println("Trying to leave: " + player);
        
        for (var p : getPlayers())
            p.ready = 0;
        
        if (player == null || !players.containsKey(player.id))
            return;

        System.out.println("Leaving " + player.name);

        var me = player.equals(this.player);

        if (remote != null) {
            if (send) {
                if (kick)
                    remote.push(Message.msgKick(player));
                else
                    remote.push(Message.msgLeave(player));
            }

            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                remote.way.leave(player);
            });

            if (me) {
                remote.running = false;
                // close down if lan
                if (isGameOver() || (!remote.way.isSteam() && player.isHost())) {
                    game.leaveGame = true;
                    ReplayVisual.saveReplay(this);
                    return;
                }
            }
        }
        // myself
        if (me) {
            ReplayVisual.saveReplay(this);
            return;
        }

        boolean startedGame = gm.isGameBegun();

        player.readyTime = 0;
        player.joined = false;

        remote.clearLoopsOffPlayer(player, gameID);
        players.remove(player.id);
        sortedPlayers.remove(player);
        if (this.player != null && this.player.isHost())
            sendChat(null, player.name + " left the game.");
        if (Features.inst != null)
            Features.inst.getAudio().play(SfxTypes.LEFT);

        // was host
        if (player.isHost() && isSteam()) {
            if (!Main.DEBUG) {
                if (!startedGame) {
                    game.endAll();
                    return;
                }
            }

            var replacement = replacement();
            if (replacement != null) {
                replacement.role = Player.HOST;
                player.role = Player.PLAYER;
                if (isSteam())
                    Features.inst.setLobbyOwner(replacement);
                System.out.println("NEW HOST: " + replacement.name);
            }
        }

        if (startedGame && !isGameOver()) {
            finishControl();
            if (!kick) {
                player.timeLapsedInRace = -1;
                countdownPaused = true;
                lostPlayers.put(player.steamID, player);
                var round = paidAtRound;
                gm.rewardPlayer(round, -1, -1, 0, -1, player, false);

                if (this.player.isHost() && isGameStarted() && remote.way instanceof LocalRemote2 lanRemote) {
                    lanRemote.openUDP();
                }
            }
        } else {
            gm.newEndGoal();
        }

        updatePlayersIndices();
        updateLobbyString();
    }

    public void imLeaving() {
        leave(this.player, true, false);
    }

    public void ready(Player player, byte val) {
        player.ready = val;
        player.readyTime++;
        updateLobbyString();
        if (remote != null)
            remote.push(Message.msgReady(player, player.readyTime, player.ready));
    }

    public void updateLobbyString() {
        if (actionLobbyString == null)
            return;
        var playerInfos = new ArrayList<PlayerLobbyInfo>();

        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player player = sortedPlayers.get(i);
            if (player.role != Player.COMMENTATOR || sortedPlayers.size() == 1) {

                playerInfos.add(new PlayerLobbyInfo(player.getLobbyInfo(), player.getCarInfo(),
                        player.isReady() && !gm.isRacing()));
            }
        }

        actionLobbyString.run(playerInfos, gm.getEndGoalText());
    }

    public String updateRaceLobby(boolean force) {
        if (!raceLobbyStringFinalized) {
            raceLobbyString = updateRaceLobby(false, force);
        }

        return raceLobbyString;
    }

    /**
     * @return name#ready#car#...
     */
    private String updateRaceLobby(boolean allFinished, boolean full) {
        var result = new StringBuilder();

//        if (!allFinished) {
//            // Hent spillere i hvilken som helst rekkefølge og sett de inn i
//            // returnstrengen
//            result.append(0);
//            for (var player : players.values()) {
//                if (player.role != Player.COMMENTATOR) {
//                    result.append("#").append(player.getRaceInfo(false, full, isSingleplayer()));
//                }
//            }
//
//            return result.toString();
//        }

        result.append(1);

        // Sorter alle spillere etter alle har fullført racet
        var sortedByTime = new LinkedList<>(players.values());
        sortedByTime.sort((o1, o2) -> {
            if (!full) {
                if (o1.finished == 0 && o2.finished == 1) {
                    return 1;
                } else if (o1.finished == 1 && o2.finished == 0) {
                    return -1;
                } else if (o1.timeLapsedInRace < o2.timeLapsedInRace) {
                    if (o1.timeLapsedInRace >= 0)
                        return -1;
                    else
                        return 1;
                } else if (o1.timeLapsedInRace > o2.timeLapsedInRace) {
                    if (o2.timeLapsedInRace >= 0)
                        return 1;
                    else
                        return -1;
                }
            } else {
                if (o1.bank.getLong(Bank.POINT) < o2.bank.getLong(Bank.POINT)) {
                    return 1;
                } else if (o1.bank.getLong(Bank.POINT) > o2.bank.getLong(Bank.POINT)) {
                    return -1;
                }
            }
            return 0;
        });

        // Legg de inn i strengen
        int n = 0;
        for (var value : sortedByTime) {
            if (value.role == Player.COMMENTATOR)
                continue;

            var str = value.getRaceInfo(allFinished, full, isSingleplayer());
            n++;
            result.append("#").append(n).append(": ").append(str);
        }
        return result.toString();
    }

    public long[] getRaceLights() {
        return raceLights;
    }

    public String getRaceLightsString() {
        StringBuilder sb = new StringBuilder();

        for (long time : raceLights)
            sb.append(time).append(Translator.split);

        sb.setLength(sb.length() - 1);

        return sb.toString();

    }

    public void setRaceLightsString(String[] input, AtomicInteger index) {
        long[] timesResult = new long[GameMode.raceLightsLength];
        for (int i = 0; i < timesResult.length; i++) {
            timesResult[i] = Long.parseLong(input[index.getAndIncrement()]);
        }
        this.raceLights = timesResult;
    }

    /**
     * Is it safe to gather the times of racelights?
     */
    public boolean updateRaceLights() {

        if (player != null && player.isHost()) {
            raceLights = gm.createWaitTimeRaceLights();
            if (remote != null)
                remote.push(Message.msgRaceLights(getRaceLightsString()));
        }

        return raceLights != null;
    }

    public void setCountdown(long countdown) {
        this.countdown = countdown;
    }

    public long getCountdown() {
        return countdown;
    }

    public void roundRaceCountdown() {
        var prevTime = (countdown - System.currentTimeMillis()) / 1000;
        long time;

        if (prevTime < 0 || prevTime > gm.countdownSetting - 5) {
            time = gm.countdownSetting;
        } else {
            time = (long) (Math.floor((prevTime - 1d) / 10d) * 10d + 10d);
        }
        countdown = System.currentTimeMillis() + time * 1000L;
    }

    public void updateRaceCountdown(Player host, boolean force, boolean justSendIt) {
        if (host != null && host.isHost() && (force || isRaceCountdownNotUpdated())) {
            if (!justSendIt && isGameStarted())
                roundRaceCountdown();
            if (remote != null) {
                remote.push(Message.msgTempStatus(gmCreationTime, gm, countdown, System.currentTimeMillis(), getPlayers()));
            }
        }
    }

    public boolean isRaceCountdownNotUpdated() {
        return countdown < System.currentTimeMillis();
    }

    public void clearRaceCountdown() {
        countdown = -1;
    }

    public boolean isCountdownPaused() {
        if (isEveryoneReady())
            return false;

        var res = !gm.isGameBegun() || countdownPaused || countdown == -1 || raceLights == null;
        if (!res) {
            for (var player : players.values()) {
                if (player.isPlayer() && player.isReady() && !player.inTheRace) {
                    return false;
                }
            }
        }

        roundRaceCountdown();
        return true;
    }

    public void setCountdownPaused(boolean countdownPaused) {
        this.countdownPaused = countdownPaused;
    }

    public synchronized boolean startRace(long startTime) {
        // if (player == null || player.isHost()) {
        if (gm.isGameExplicitlyOver())
            return false;
        gm.startNewRace(startTime);
        gm.setRacing(true);
        raceLobbyStringFinalized = false;
        if (!isSingleplayer() && player != null) {
            if (player.isHost()) {
                remote.push(Message.msgRaceStarted(gm.getRound()));
            } else if (isLAN() && getHost().resigned) {
                var replacement = replacement();
                if (replacement != null && replacement == truePlayer) {
                    remote.push(Message.msgRaceStarted(gm.getRound()));
                }
            }
        }
//        paidAtRound = (short) gm.getRound();
        return true;
    }

    public int getTrackLength() {
        return gm.getRaceGoal();
    }

    public String getWinner(Player player) {
        if (player.resigned)
            return "You resigned!#" + UIColors.WHITE;
        return gm.getDeterminedWinnerText(player);
    }

    public void addChatFromServer(String text) {
        if (actionNewChat != null) {
            actionNewChat.accept(text);
        }
    }

    public Message createChat(Player player, String text) {
        text = (player != null ? (player.name.length() >= 20 ? player.name.substring(0, 20) + "." : player.name) + ": "
                : "") + text;
        addChatFromServer(text);
        return Message.msgChat(text);
    }

    public void sendBeep(Player player) {
        if (remote != null) {
            remote.push(Message.msgBeep(player));
        }
    }

    public void sendChat(Player player, String text) {
        if (remote == null)
            return;
        remote.push(createChat(player, text));
    }

    public void sendChat(String text) {
        sendChat(player, text);
    }

//    public String getChat(Player player) {
//        String chatText = null;
//
//        if (player != null) {
//            var mailbox = mail.get(player);
//            if (mailbox != null)
//                chatText = mailbox.getChat();
//        }
//        return chatText;
//    }
//
//    public String getChat() {
//        return getChat(player);
//    }

    public void updateCloneToServer(Player player, String cloneString) {
        var addedHistory = player.addHistory(cloneString, false, gm.getRound());
        if (remote != null) {
            remote.push(Message.msgUpdateClone(player, addedHistory.cloneString(), addedHistory.index()));
        }
        if (isCoop()) {
            for (var p : players.values()) {
                if (!p.equals(player)) {
                    Translator.setCloneString(p.getCarRep(), player.getCarRep());
                }
            }
        }
        updateLobbyString();
    }

    public void updateCarCloneToServer(Player player) {
        if (isCoop()) {

            for (var p : sortedPlayers) {
                if (p.id == player.id)
                    continue;
                Translator.setCloneString(p.getCarRep(), player.getCarRep());
                p.redoLastHistory();
            }
            if (bonusModal.isVisible()) {
                bonusModal.setCombination(this.player);
            }
        }

        updateLobbyString();
        if (actionRemakeUpgradeView != null)
            actionRemakeUpgradeView.run();
    }

    public boolean isGameOverPossible() {
        return gm == null || gm.isGameOverPossible();
    }

    public boolean isGameOver() {
        return gm == null || gm.isGameExplicitlyOver();
    }

    public boolean isGameStarted() {
        return gm != null && gm.isGameBegun();
    }

    public static void sortPlayers(List<Player> sortedPlayers) {
        //noinspection ComparatorMethodParameterNotUsed
        sortedPlayers.sort((p1, p2) -> {
            if (p1.bank.getLong(Bank.POINT) < p2.bank.getLong(Bank.POINT))
                return 1;
            else if (p1.bank.getLong(Bank.POINT) > p2.bank.getLong(Bank.POINT))
                return -1;
            else
                return p1.id > p2.id ? 1 : -1;
        });
    }

    public void updateSortedPlayers() {
        //
        // // Sorter alle spillere etter alle har fullført racet
        sortedPlayers.clear();
        for (var p : players.values()) {
            if (p.role != Player.COMMENTATOR)
                sortedPlayers.add(p);
        }
        if (!sortedPlayers.isEmpty()) {
            sortPlayers(sortedPlayers);
        }

        updateLobbyString();
    }

    private static void setPodiumAndAheadBy(Player player, Player[] playersIncludingLost) {
        int place = 0;
        long aheadBy = -2000;
        long myPoints = player.bank.getLong(Bank.POINT);

        for (Player other : playersIncludingLost) {
            if (other != player) {
                long difference = myPoints - other.bank.getLong(Bank.POINT);
                if (difference > aheadBy) {
                    aheadBy = difference;
                }

                if (difference < 0) {
                    place++; // place as in where you are overall.
                }
            }
        }
        player.aheadByPoints = (int) aheadBy;
        player.podium = place; // skriver bare seg selv og kj�res hver tick!
    }

    public static void determinePositioningFinishedRace(GameMode gm,
                                                        Player[] players,
                                                        int round,
                                                        Player me) {
        // Update game information about players:
        for (var player : players) {
            if (player.resigned) {
                continue;
            }

            int place = 0;
            var thisTime = player.timeLapsedInRace;
            long timeBehindFirst = 0;
            var pointsBehindFirst = 0;

            // Disconnected players and dnfers gain some money:
            if (thisTime == Race.CHEATED_TOO_EARLY || thisTime == Race.CHEATED_GAVE_IN) {
                gm.rewardPlayer(round, -1, players.length, 0, -1, player, player == me);
            } else {
                for (var otherPlayer : players) {
                    // not same player
                    if (!otherPlayer.resigned && otherPlayer.id != player.id) {
                        var otherTime = otherPlayer.timeLapsedInRace;
                        if (thisTime > otherTime && otherTime >= Race.CHEATED_NOT) {
                            place++; // place as in place in this race. About
                            // how fast you were now, not where you
                            // are in regards to the actual game
                            var timeBehind = thisTime - otherTime;
                            if (timeBehind > timeBehindFirst) {
                                timeBehindFirst = timeBehind;
                            }
                        }
                        int pointsDiff = (int) (otherPlayer.bank.getLong(Bank.POINT) - player.bank.getLong(Bank.POINT));
                        if (pointsDiff > pointsBehindFirst) {
                            pointsBehindFirst = pointsDiff;
                        }
                    }
                }

                if (player.fastestTimeLapsedInRace == -1 || player.timeLapsedInRace < player.fastestTimeLapsedInRace) // && not cheated
                    player.fastestTimeLapsedInRace = player.timeLapsedInRace;
                player.podiumRace = place;
                gm.rewardPlayer(round, place, players.length, pointsBehindFirst, timeBehindFirst, player,
                        player == me);
//				System.out.println(player.name + " ahead by " + player.aheadByPoints);
            }
            if (round != 0)
            	player.addHistory(Translator.getCloneString(player), round+1);
            else
            	player.replaceLastHistory(round+1);
        }

        for (var player : players) {
            setPodiumAndAheadBy(player, players); // Denne fungerer ikke skikkelig
//			System.out.println(
//					player.name + " ahead by " + player.aheadByPoints + " p with time: " + player.timeLapsedInRace);
        }
    }

    public void win() {
        gm.determineWinner();
        gm.endGameToWin();
        raceLobbyString = updateRaceLobby(true, true);
        if (player.isHost())
			Features.inst.endLobby();
        SceneHandler.changeScene(Scenes.RACE, false);
    }

    public void resign() {
        resigned = true;
        resign(player);
        remote.push(Message.msgResign(player));
//        gm.determineWinner();
//        gm.endGameToWin();
        raceLobbyString = updateRaceLobby(true, true);
    }

    public void resign(Player player) {
        raceLobbyStringFinalized = false;
        player.resigned = true;
        player.podiumRace = -1;
        player.timeLapsedInRace = Race.CHEATED_GAVE_IN;
        player.bank.added[Bank.MONEY] = 0;
        player.bank.added[Bank.POINT] = 0;
    }

    private synchronized void finishControl() {
        if (gm != null && gm.isAllFinished()) {
            System.out.println("stop race on the server");
            gm.setRacing(false);
            determinePositioningFinishedRace(gm, getAllPlayersExceptCommentator(), paidAtRound, player);
            paidAtRound++;
            updateSortedPlayers();

            raceLobbyStringFinalized = true;

            if (gm.isGameOver()) {
                win();
            } else {
                raceLobbyString = updateRaceLobby(true, false);
                gm.resetAllFinished();
                updateRaceLights();
                if (player != null && player.isHost()) {
                    gm.hostDoStuff();
                    gm.prepareNextRaceManually(gm.getNewRaceGoal());
                    raceSeed = System.currentTimeMillis();
                    sendGamemodeAllInfo();
                }
                updateLobbyString();
            }

            if (actionEveryoneFinished != null)
                actionEveryoneFinished.run();

            if (player != null && player.isHost())
                runUpdatedGameModeAction();
        }
    }

    public void finishRace(Player player, long time, int speed) {
        if (!resigned && this.player.equals(player)) {
        	finishedAtRound++;
            Tool.runToolAfterRace(player, paidAtRound);
            store.resetTowardsPlayer(player);
            player.layer.slowlyResetDevastation();
            gm.setRacing(false);
            if (remote != null)
                remote.push(Message.msgFinishRace(player, time, speed));
        }

        player.timeLapsedInRace = time;
        player.finished = 1;
        player.ready = 0;
        player.inTheRace = false;
        player.inTheRaceTime++;
        if (actionFinishPlayerAnimation != null) {
            player.car.getStats().finishSpeed = speed;
            actionFinishPlayerAnimation.run(player.car, time < Race.CHEATED_NOT, speed);
        }
        player.car.reset(player.layer);

        finishControl();
    }

    public void goInTheRace(Player player) {
        player.inTheRace = true;
        player.inTheRaceTime++;

        if (remote != null && this.player.equals(player))
            remote.push(Message.msgSetInRace(player, true));
    }

    public Player getPlayer(String[] input, int fromIndex) {
        int othersGameID = Integer.parseInt(input[fromIndex + 1]);
//		System.out.println("mine: " + this.gameID + ", " + othersGameID);
        if (this.gameID != -1 && othersGameID != this.gameID) {
            System.out.println("wrong gameID");
            return null;
        }
        return getPlayer(Byte.parseByte(input[fromIndex]));
    }

    public Player getPlayer(byte id) {
        return players.get(id);
    }

    public Player getPlayerSteamId(long steamID) {
        if (steamID != 0) {
            for (var other : players.values()) {
                if (other.steamID != 0 && steamID == other.steamID) {
                    return other;
                }
            }
        }
        return null;
    }

    public GameMode getGamemode() {
        return gm;
    }

    public synchronized boolean runUpdatedGameModeAction() {
        if (gm == null)
            return false;

        if (actionGameModeUpdated != null)
            actionGameModeUpdated.accept(gm);
        return true;
    }

    public static GameMode createGameMode(GameModes gamemode, int singleplayerArg) {
        return switch (gamemode) {
        	case TOTAL:
        		yield new TotalMode(gamemode, 9, 4, "Total");
            case SHORT_TOTAL:
                yield new TotalMode(gamemode, 5, 3, "Short Total");
            case MID_TOTAL:
                yield new TotalMode(gamemode, 12, 5, "Medium Total");
            case LONG_TOTAL:
                yield new TotalMode(gamemode, 18, 6, "Long Total");
            case MEGA_LONG_TOTAL:
                yield new TotalMode(gamemode, 36, 7, "Mega Long Total");
            case TOTAL_NO_UPGRADES:
                yield new NoUpgradeMode(gamemode, 5, "Total: No Upgrading");
            case LONG_TOTAL_NO_UPGRADES:
                yield new NoUpgradeMode(gamemode, 10, "Long Total: No Upgrading");
            case MEGA_LONG_TOTAL_NO_UPGRADES:
                yield new NoUpgradeMode(gamemode, 20, "Mega Long Total: No Upgrading");
            case LEADOUT:
                yield new LeadoutMode(gamemode, false, 3, "Leadout");
            case BELOW_MEDIUM_LEADOUT:
                yield new LeadoutMode(gamemode, false, 4, "Below Medium Leadout");
            case MEDIUM_LEADOUT:
                yield new LeadoutMode(gamemode, false, 6, "Medium Leadout");
            case LONG_LEADOUT:
                yield new LeadoutMode(gamemode, false, 10, "Long Leadout");
            case HARDCORE_LEADOUT:
                yield new LeadoutMode(gamemode, true, 3, "Hardcore Leadout");
            case BELOW_MEDIUM_HARDCORE_LEADOUT:
                yield new LeadoutMode(gamemode, true, 4, "Below Medium Hardcore Leadout");
            case MEDIUM_HARDCORE_LEADOUT:
                yield new LeadoutMode(gamemode, true, 6, "Medium Hardcore Leadout");
            case LONG_HARDCORE_LEADOUT:
                yield new LeadoutMode(gamemode, true, 10, "Long Hardcore Leadout");
            case FIRST_TO_3:
                yield new FirstToMode(3);
            case FIRST_TO_5:
                yield new FirstToMode(5);
            case FIRST_TO_7:
                yield new FirstToMode(7);
            case FIRST_TO_9:
                yield new FirstToMode(9);
            case FIRST_TO_12:
                yield new FirstToMode(12);
            case FIRST_TO_18:
                yield new FirstToMode(18);
            case TIME_ATTACK:
                yield new TimeMode(gamemode, 3500, "Time-attack");
            case PRETTY_FAST_TIME_ATTACK:
                yield new TimeMode(gamemode, 2000, "Pretty fast time-attack");
            case LIGHTNING_TIME_ATTACK:
                yield new TimeMode(gamemode, 600, "Lightning time-attack");
            case DARN_FAST_TIME_ATTACK:
                yield new TimeMode(gamemode, 200, "Darn fast time-attack");
            case INSANE_TIME_ATTACK:
                yield new TimeMode(gamemode, 25, "Insane time-attack");
            case COOP_FUN:
                yield new CoopMode(gamemode, SingleplayerChallenges.DailyFun, "Co-op Fun");
            case COOP_WEIRD:
                yield new CoopMode(gamemode, SingleplayerChallenges.DailyWeird, "Co-op Weird");
            case COOP_TOUGH:
                yield new CoopMode(gamemode, SingleplayerChallenges.DailyTough, "Co-op Tough");
            case SANDBOX:
                yield new SandboxMode();
            case CLASSIC:
                yield new ClassicMode(gamemode);
            case SINGLEPLAYER_CHALLENGES:
                yield new SingleplayerChallengesMode(singleplayerArg);
            default:
                throw new IllegalArgumentException("Unexpected value: " + gamemode);
        };
    }

    public synchronized GameMode init(GameModes gamemode, int arg, String[] input, AtomicInteger index) {
        if (input != null)
            gamemode = GameModes.values()[Integer.parseInt(input[index.getAndIncrement()])];
        else if (gamemode == null)
            gamemode = type.isSinglePlayer() ? GameModes.SINGLEPLAYER_CHALLENGES : GameModes.values()[gamemodeScrollingIndex];

        gm = createGameMode(gamemode, arg);
        gm.setPlayers(players);

        if (gm instanceof SandboxMode sandbox) {
            sandbox.store = store;
        }

        if (player.isHost()) {
	        player.layer.trueReset();
	        player.upgrades.resetTowardsCar(player.getCarRep(), player.layer);
	        store.resetTowardsPlayer(player);
	        var addedHistory = player.addHistory(Translator.getCloneString(player), true, gm.getRound());
	        if (remote != null) {
	            remote.push(Message.msgUpdateClone(player, addedHistory.cloneString(), addedHistory.index()));
	        }
        }

        if (!player.isHost()) {
            if (input != null)
                gm.setAllInfo(player, input, index);
            if (isCoop()) {
                Translator.setCloneString(player.layer, new Layer());
            } else if (!isGameStarted()) {
                var host = getHost();
                if (host != null)
                    Translator.setCloneString(player.layer, host.layer);
            }
            store.createRemoveTiles(player);
        } else if (input != null) {
            gm.setAllInfo(player, input, index);
        } else {
            gm.createPrices(player, Upgrade.priceFactorStd);
            gm.newEndGoal();
            gm.hostDoStuff();
        }

        gm.updateInfo();

        for (var player : players.values()) {
            gm.setPrices(player.upgrades);
            player.ready = 0;
        }
        updateLobbyString();
        runUpdatedGameModeAction();

        return gm;
    }

    /**
     * scrolls through gamemodes with +1 || -1
     * <p>
     * Only host can do this
     */
    public void init(int i) {
        int max;
        int min;
        if (type.isSinglePlayer()) {
            min = 0;
            max = GameModes.getSingleplayerModesCount() - 1;
        } else {
            min = GameModes.getSingleplayerModesCount();
            max = min + GameModes.getMultiplayerModesCount() - 1;
        }

        gamemodeScrollingIndex += i;
        if (gamemodeScrollingIndex < min)
            gamemodeScrollingIndex = max;
        else if (gamemodeScrollingIndex > max)
            gamemodeScrollingIndex = min;

        var gamemode = GameModes.values()[gamemodeScrollingIndex];

        init(gamemode, 0, null, null);
        gmCreationTime++;
        if (remote != null)
            remote.push(Message.msgGameModeChange(gm, gmCreationTime));
    }

    public boolean isSteam() {
        return type.isSteam();
    }

    public Player[] getPlayers() {
        return players.values().toArray(new Player[0]);
    }

    /**
     * For testing
     */
    public Player addPlayer(Player player) {
        player.gameID = gameID;
        var newPlayer = new Player();
        Translator.setCloneString(newPlayer, player);
        players.put(player.id, newPlayer);
        return newPlayer;
    }

    public Player[] getPlayersIncludingLostOnes() {
        var res = new ArrayList<Player>();
        res.addAll(players.values());
        res.addAll(lostPlayers.values());
        return res.toArray(new Player[0]);
    }

    public Player[] getAllPlayersExceptCommentator() {
        var psTemp = getPlayersIncludingLostOnes();
        var ps = new ArrayList<Player>();
        for (int i = 0; i < psTemp.length; i++) {
            if (psTemp[i].role != Player.COMMENTATOR)
                ps.add(psTemp[i]);
        }
        return ps.toArray(new Player[0]);
    }

    public void setGameID(int gameID) {
        this.gameID = gameID;
    }

//	public void mailEveryoneButHost(String letter) {
//		for (var player : players.values()) {
//			// dont tellhimself
//			if (!player.isHost()) {
//				mail.get(player).addMail(letter);
//			}
//		}
//	}

    public boolean doubleCheckStartedRace() {
        return gm.isRacing() || gm.everyoneInRace();
    }

    public void setChosenCarModels() {
        for (Player player : getPlayersIncludingLostOnes()) {
            player.car.getModel().setModel(player.getCarNameID());
        }
    }

    public void setChosenCarAudio(AudioRemote audio) {
        for (var player : getPlayersIncludingLostOnes()) {
            if (player.car.getAudio() == null) {
                player.car.setAudio(audio.getNewCarAudio(player.car.getRep().getName()));
                System.out.println("players: " + player);
            }
        }
    }

    public void raceInformation(Player player, float distance, int speed, double spdinc, boolean brake, long raceTime, boolean me) {
        if (!gm.isRacing())
            return;

        if (!player.car.getModel().pushInformation(distance, speed, spdinc, raceTime) && brake == player.car.getStats().visualBrake)
			return; // no change to update
        player.car.getStats().visualBrake = brake;
        if (!me) {
        	player.car.brake(brake);
        }else if (remote != null) {
            remote.push(Message.msgRaceInfo(player, distance, speed, spdinc, brake, raceTime));
        }
    }

    public void sendGamemodeAllInfo() {
        if (remote != null)
            remote.push(Message.msgGameModeAllInfo(player, gm, raceSeed));
    }

    public boolean isEveryoneDone() {
        return raceLobbyStringFinalized;
    }

    public boolean isEveryoneReady() {
        for (var player : players.values()) {
            if (!player.resigned && !player.isReady()
                    && (player.role != Player.COMMENTATOR || (!gm.isGameBegun() && player.role == Player.COMMENTATOR)))
                return false;
        }
        return true;
    }

    public List<Player> getSortedPlayers() {
        return sortedPlayers;
    }

    public List<Player> getSortedPlayersExtra() {
        return gm.addExtraPlayers(sortedPlayers);
    }

    public boolean isSingleplayer() {
        return remote == null;
    }

    public void undoHistory(Player player, int index, boolean redo) {
        if (remote != null)
            remote.push(Message.msgUndo(player, index, player.undoTime, gm.getRound(), redo));
    }

    public void winClose() {
        if (bonusModal != null) {
            bonusModal.cancel(null);
            bonusModal.hide();
        }

        for (var p : getPlayersIncludingLostOnes()) {
            var audio = p.car.getAudio();
            if (audio != null)
                audio.delete();
        }
    }

    public void close() {
        exists = false;

        if (remote != null)
            remote.running = false;
        if (game != null)
        	game.leaveGame = true;

        winClose();
    }

    public void updatePlayersIndices() {
        var keys = players.keySet().toArray(new Byte[]{});
        var taken = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) {
            if (taken[i])
                continue;
            updatePlayerIndex(i, keys, taken);
        }
    }

    private void updatePlayerIndex(int i, Byte[] keys, boolean[] taken) {
        var falseKey = keys[i];
        var player = players.get(falseKey);
        byte actualKey = player.id;
        if (falseKey != actualKey) {
            // om det er en i veien; slett deg selv slik at den kan evt flytte inn der du er
            // for at du s� kan ta dens plass.
            players.remove(falseKey);
            taken[i] = true;

            // flytt en som er i veien.
            var existingPlayer = players.get(actualKey);
            if (existingPlayer != null) {
                for (; i < keys.length; i++)
                    if (Objects.equals(keys[i], actualKey))
                        break;
                if (i < keys.length)
                    updatePlayerIndex(i, keys, taken);
            }

            players.put(actualKey, player);
        }
    }

    public int getRound() {
        return gm == null ? overrideRound : gm.getRound();
    }

    public int getGameID() {
        return gameID;
    }

    public UpgradeResult attemptImproveTile(Player player, Upgrade upgrade, int x, int y) {
        var result = store.attemptImproveUpgrade(player, upgrade, x, y);
        if (result != UpgradeResult.DidntGoThrough) {
            updateCloneToServer(player, Translator.getCloneString(player));
        }
        return result;
    }

    public UpgradeResult attemptBuyTile(Player player, TilePiece<?> tile) {
        var result = store.attemptBuyTile(player, tile, gm.getRound());
        if (result == UpgradeResult.Bought) {
            updateCloneToServer(player, Translator.getCloneString(player));
        }

        return result;
    }

    public void setActionUpdatedGameMode(Consumer<GameMode> gameModeUpdatedAction) {
        this.actionGameModeUpdated = gameModeUpdatedAction;
    }

    public void setActionFinishPlayerAnimation(IActionTriple<Car, Boolean, Integer> finishPlayerAnimationAction) {
        this.actionFinishPlayerAnimation = finishPlayerAnimationAction;
    }

    public void setActionEveryoneFinished(IAction action) {
        this.actionEveryoneFinished = action;
    }

    public IAction getActionEveryoneFinished() {
        return actionEveryoneFinished;
    }

    public Remote2 getRemote() {
        return remote;
    }
    public boolean isLAN() {
        return !type.isSteam() && !type.isSinglePlayer();
    }

    public void setActionUpdateLobbyString(IActionDouble<List<PlayerLobbyInfo>, String> updateLobby) {
        actionLobbyString = updateLobby;
    }

    public void setActionNewChat(Consumer<String> actionNewChat) {
        this.actionNewChat = actionNewChat;
    }

    public boolean isSingleplayerLost() {
        return isSingleplayer() && !gm.isWinner(player);
    }

    public boolean isSingleplayerLostLastRace() {
        return isSingleplayer() && ((SingleplayerChallengesMode) gm).spLostRace;
    }

    public boolean isCoop() {
        return gm instanceof CoopMode;
    }

    public void setActionRemakeUpgradeView(IAction actionRemakeUpgradeView) {
        this.actionRemakeUpgradeView = actionRemakeUpgradeView;
    }

    public short getPaymentRound() {
        return paidAtRound;
    }

    @Override
    public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
        if (lvlDeep > 0)
            outString.append(splitter);
//        lvlDeep++;
        outString
                .append(gm.getAllInfo())
                .append(splitter).append(gmCreationTime)
                .append(splitter).append(countdownPaused ? 1 : 0)
                .append(splitter).append(countdown)
                .append(splitter).append(gameID)
                .append(splitter).append(paidAtRound)
                .append(splitter).append(getRaceLightsString());
    }

    @Override
    public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
        init(null, 0, cloneString, fromIndex);
        gmCreationTime = Long.parseLong(cloneString[fromIndex.getAndIncrement()]);
        countdownPaused = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) != 0;
        setCountdown(Long.parseLong(cloneString[fromIndex.getAndIncrement()]));
        gameID = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
        paidAtRound = Short.parseShort(cloneString[fromIndex.getAndIncrement()]);
        setRaceLightsString(cloneString, fromIndex);
    }

    public void clearHost() {
        var host = getHost();
        if (host == null)
            return;
        players.remove(host.id);
    }

    public void rejoin(String[] input, AtomicInteger index) {
        var otherPlayer = new Player();
        otherPlayer.setCloneString(input, index);
        var existingOtherPlayer = join(otherPlayer.getClone());
        Translator.setCloneString(existingOtherPlayer, otherPlayer);
        existingOtherPlayer.joined = true;
        updatePlayersIndices();
    }

	public boolean isBehindRounds() {
		return finishedAtRound < gm.getRound();
	}

	public void setGameMode(GameMode gm) {
		this.gm = gm;
	}

    public void sendStatusUpdate() {
        if (remote != null && !resigned)
            remote.push(Message.msgMyPlayerStatus(player));
    }

}