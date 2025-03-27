package communication;

import adt.ICloneStringable;
import comNew.MessageType;
import comNew.RemoteInfo;
import communication.remote.Message;
import game_modes.GameModes;
import main.Main;
import player_local.Player;
import player_local.car.Rep;
import scenes.SceneHandler;
import scenes.Scenes;

import java.util.concurrent.atomic.AtomicInteger;

import static communication.ResponseState.FAIL;
import static communication.ResponseState.KEEP_BACK;

/*

Lagre meldinger som kommer inn som gjelder spillere som ikke ennå har blitt laget.
Helt til kampen har startet?

Send med index på undo og history/getPlayerAllInfo

Ha "time" ved at hvor hver endring så økes tid med 1.
Dette skal gjelde:
- ready
- changeGamemode
- selectedCarIndex
- newEndGoal

 */

public class Translator {
    //    private Consumer<Player> afterJoined;
//    private final Queue<Message> afterJoinedEarlyCalls = new LinkedList<>();
//    private final GameInfo info;
//    private final Remote remote;
    public static final char
            mailResAll = '0',
            mailRes = '1',
            mailRequest = '2';
    public static final String
            join = "join",
            joinRes = "joinRes",
            joinTell = "joinTell",
            joined = "joined",
            finish = "finish",
            inTheRace = "inRace",
            raceInformation = "raceInf",
            leave = "leave",
            kick = "kick",
            raceLights = "lights",
            tempStatus = "countdown",
            myStatus = "mystatus",
            getWinner = "win",
            updateClone = "carRep",
            ready = "ready",
            isGameOver = "gameOver",
            isGameStartedAlready = "gamebegun",
            changeGamemode = "gmChange",
            afterRaceGamemodeUpdate = "gmInf",
            undo = "undo",
            selectedCarIndex = "selCar",
            raceStarted = "raceStart",
            chat = "chat",
            newEndGoal = "endGoal",
            resign = "resign",
            beep = "beep";
    public static final String split = "#",
            specialSplit = "`",
            hashtag = "ଢ",
            newLine = "soidauyoiuv",
            charBreak = "a2sdlkj";

    public static MessageType whichMessageType(String message) {
    	if (message.isEmpty()) 
    		return null;
        return switch (message.charAt(0)) {
            case mailRes, mailResAll -> MessageType.response;
            case mailRequest -> MessageType.request;
            default -> null;
        };
    }

    public static boolean isForAll(String message) {
        return switch (message.charAt(0)) {
            case mailResAll -> true;
            default -> false;
        };
    }

    /*
     * REQUEST
     */

    /**
     * take the first word and run the rest to its responsible function. Like
     * SQL. If res (response) is null then a return call won't be made to the requester.
     * <p>
     * Old example but ye:
     * JOIN#name+id#host-boolean#carname LEAVE#name+id CLOSE
     * UPDATELOBBY#name+id#ready UPDATERACE#name+id#mysitsh
     */
    public static String understandRequest(
            Message message,
            RemoteInfo info
    ) {
        if (message.requestMessage.isBlank())
            return Response.ENDALL;

        String[] input = message.requestMessage.split(split);
        String identifier = input[0].substring(1);
        var sb = new StringBuilder();
        var index = new AtomicInteger(1);

        try {

            switch (identifier) {
                case join -> {
                    if (!input[index.get()].equals(Main.VERSION)) {
                        System.out.println("Reason for ending: Wrong version number " + input[index.get()]);
                        return Response.ENDALL;
                    }

                    sb.append(mailRequest).append(joinRes).append(split);

                    // Request data:
                    var name = input[index.incrementAndGet()].replaceAll(hashtag, split);
                    var role = Byte.parseByte(input[index.incrementAndGet()]);
                    var requesterID = Long.parseLong(input[index.incrementAndGet()]);
                    var player = new Player(name, Player.DEFAULT_ID, role, requesterID);

                    // Actually join:
                    player = info.gameInfo.join(player);
                    if (player == null)
                        return Response.ENDALL;

                    info.gameInfo.updatePlayersIndices();

                    // Game-state:
                    sb.append(info.gameInfo.raceSeed).append(split);
                    player.getCloneString(sb, 0, split, false);

                    var players = info.gameInfo.getPlayers();
                    var otherPlayersSb = new StringBuilder();
                    int otherPlayersAmount = 0;
                    for (var otherPlayer : players) {
                        if (otherPlayer.equals(player) || !otherPlayer.joined)
                            continue;
                        otherPlayer.getCloneString(otherPlayersSb, 1, split, false);
                        otherPlayersAmount++;
                    }
                    sb.append(split).append(otherPlayersAmount).append(otherPlayersSb);

                    info.gameInfo.getCloneString(sb, 1, split, false);
                    message.requesterID = player.steamID;
                    if (message.connectedLine != null)
                        message.connectedLine.senderID = player.steamID;

                    info.gameInfo.sendChat(null, player.name + " is joining...");
                }
                case joinRes -> {
                	info.gameInfo.raceSeed = Long.parseLong(input[index.getAndIncrement()]);
                    info.gameInfo.player.setCloneString(input, index);
                    info.gameInfo.clearHost();
                    var restPlayerSize = Integer.parseInt(input[index.getAndIncrement()]);
                    for (int i = 0; i < restPlayerSize; i++) {
                        info.gameInfo.rejoin(input, index);
                    }
                    info.gameInfo.setCloneString(input, index); // FIXME sinner

                    if (message.connectedLine != null)
                        message.connectedLine.senderID = info.gameInfo.getHost().steamID;

                    for (var player : info.gameInfo.getPlayers()) {
                        player.joined = true;
                    }
                    message.requesterID = info.gameInfo.getHost().steamID;
                    info.gameInfo.updateSortedPlayers();

                    System.out.println("Joined a server. Running afterJoined: " + info.afterJoined);
                    if (info.afterJoined != null) {
                        info.afterJoined.accept(info.gameInfo.player);
                    }
                    return Message.msgJoined(info.gameInfo.player);
                }
                default -> {
                    System.out.println("false start of request");
                    return Response.ENDALL;
                }
            }
        } catch (Exception e) {
            System.out.println("Failed at request! " + e.getMessage());
            return Response.ENDALL;
        }
        return sb.toString();
    }

    private static Player getPlayer(RemoteInfo info, String[] input, AtomicInteger index) {
        return info.gameInfo.getPlayer(input, index.getAndAdd(2));
    }

    /*
     * RESPONSE
     */

    /**
     * take the first word and run the rest to its responsible function. Like
     * SQL.
     * <p>
     * JOIN#name+id#host-boolean#carname LEAVE#name+id CLOSE
     * UPDATELOBBY#name+id#ready UPDATERACE#name+id#mysitsh
     * <p>
     * input from client
     */
    public static ResponseState understandResponse(
            Message message,
            RemoteInfo info
    ) {
        if (message.requestMessage.isBlank()) {
            System.out.println("BLANK REQUEST!!!");
            return ResponseState.FAIL;
        }

        var fromTime = System.currentTimeMillis();

        String[] input = message.requestMessage.split(split);
        String identifier = input[0].substring(1);

        // player can only be null if you're testing the code
        if (info.gameInfo.player != null && !info.gameInfo.player.joined)
            return KEEP_BACK;

        var index = new AtomicInteger(1);
        switch (identifier) {
            case joinTell -> {
                for (var p : info.gameInfo.getPlayers()) {
                    p.ready = 0;
                    p.readyTime = 0;
                }
                info.gameInfo.rejoin(input, index);
                info.gameInfo.getGamemode().setNewEndGoal(Integer.parseInt(input[index.getAndIncrement()]));
            }
            case joined -> {
                var player = info.gameInfo.getPlayer(input, index.get());
                player.joined = true;
                System.out.println(player.name + " joined!");

                var tellOthers = Message.msgJoinTell(player, info.gameInfo.getGamemode().getEndGoal());

                var players = info.gameInfo.getPlayers();
                for (var otherPlayer : players) {
                    if (otherPlayer.equals(player) || otherPlayer.equals(info.gameInfo.player))
                        continue;
                    info.remote.push(new Message(otherPlayer.steamID, tellOthers));
                }
                info.gameInfo.sendChat(null, player.name + " joined the game!");
            }
            case leave -> info.gameInfo.leave(getPlayer(info, input, index), false, false);
            case kick -> info.gameInfo.leave(getPlayer(info, input, index), false, true);
            case resign -> {
                info.gameInfo.resign(getPlayer(info, input, index));
                int numResigned = 0;
                var players = info.gameInfo.getPlayers();
                for (var player : players) {
                    if (player.resigned)
                        numResigned++;
                }
                if (numResigned == players.length - 1) {
                    info.gameInfo.win();
                }
            }
            case raceStarted -> {
                var round = Integer.parseInt(input[index.getAndIncrement()]);
                var myRound = info.gameInfo.getRound();
                if (myRound > round || myRound+1 < round) {
                    System.err.println("unexpected round diff: myround=" + myRound + " vs. " + round);
                }
                info.gameInfo.getGamemode().setRacing(true);
            }
            case finish -> {
                var player = getPlayer(info, input, index);
                info.gameInfo.finishRace(player, Long.parseLong(input[index.getAndIncrement()]), Integer.parseInt(input[index.getAndIncrement()]));
                player.getCarRep().set(Rep.highestSpdAchived, Long.parseLong(input[index.get()]));
            }
            case ready -> {
                var player = getPlayer(info, input, index);
                var readyTime = Integer.parseInt(input[index.getAndIncrement()]);
                if (readyTime >= player.readyTime) {
                    player.readyTime = readyTime;
                    player.ready = Byte.parseByte(input[index.getAndIncrement()]);
                    info.gameInfo.updateLobbyString();
                }
            }
            case selectedCarIndex -> {
                var player = getPlayer(info, input, index);
                var carSelectionTime = Integer.parseInt(input[index.getAndIncrement()]);
                if (carSelectionTime > player.carSelectionTime) {
                    player.carSelectionTime = carSelectionTime;
                    info.gameInfo.carSelectUpdate(
                            player,
                            Byte.parseByte(input[index.getAndIncrement()]),
                            Integer.parseInt(input[index.getAndIncrement()]) != 0,
                            true);
                }
            }
            case updateClone -> {
                var player = getPlayer(info, input, index);
                if (player == null)
                    return ResponseState.FAIL;
                var histIndex = Integer.parseInt(input[index.getAndIncrement()]);
                var cloneString = message.requestMessage.substring(message.requestMessage.indexOf(split + specialSplit) + 2);
                player.addHistory(histIndex, cloneString);
                info.gameInfo.updateCarCloneToServer(player);
                info.gameInfo.updatePlayersIndices();
            }
            case inTheRace -> getPlayer(info, input, index).inTheRace = Integer.parseInt(input[index.get()]) != 0;
            case undo -> {
                var player = getPlayer(info, input, index);
                if (player == null)
                    return ResponseState.FAIL;

                var id = player.id;
                var steamId = player.steamID;
                var gameId = player.gameID;

                var history = player.getHistory();
                if (history.size() == 0)
                    return ResponseState.FAIL;

                var redo = Integer.parseInt(input[index.getAndIncrement()]) != 0;

                var histTime = Integer.parseInt(input[index.getAndIncrement()]);
                if (histTime < player.undoTime)
                    return ResponseState.FAIL;

                var histIndex = Integer.parseInt(input[index.getAndIncrement()]);
                if (histIndex == 0)
                    return ResponseState.FAIL;

                var round = Integer.parseInt(input[index.get()]);

                if (redo) {
                    if (history.size() - 1 >= histIndex)
                        player.redoHistory(histIndex);
                    else
                        player.redoLastHistory();
                } else {
                    if (history.size() - 1 >= histIndex)
                        player.undoLastHistory(round);
                    else
                        player.undoHistory(histIndex);
                }

                player.id = id;
                player.steamID = steamId;
                player.gameID = gameId;
                player.undoTime = histTime;
            }
            case raceInformation -> {
                var player = getPlayer(info, input, index);
                var distance = Float.parseFloat(input[index.getAndIncrement()]);
                var speed = Integer.parseInt(input[index.getAndIncrement()]);
                var spdinc = Double.parseDouble(input[index.getAndIncrement()]);
                var brake = Integer.parseInt(input[index.getAndIncrement()]) != 0;
                var raceTime = Long.parseLong(input[index.getAndIncrement()]);
                info.gameInfo.raceInformation(player, distance, speed, spdinc, brake, raceTime, false);
            }
            case changeGamemode -> {
                var gmTime = Long.parseLong(input[index.getAndIncrement()]);
                if (gmTime > info.gameInfo.gmCreationTime) {
                    info.gameInfo.gmCreationTime = gmTime;
                    info.gameInfo.init(null, 0, input, index);
                }
            }
            case afterRaceGamemodeUpdate -> {
                var player = getPlayer(info, input, index);
                if (player != null && player.isHost()) { // Message from host
                	info.gameInfo.raceSeed = Long.parseLong(input[index.getAndIncrement()]);
                    var gmEnum = GameModes.valueOf(input[index.getAndIncrement()]);
                    if (info.gameInfo.getGamemode() == null || gmEnum != info.gameInfo.getGamemode().getGameModeEnum()) {
                        info.gameInfo.init(null, 0, input, index);
                    } else {
                        index.incrementAndGet();
                        info.gameInfo.getGamemode().setAllInfo(player, input, index);
                    }
                }
            }
            case myStatus -> {
                var id = Byte.parseByte(input[index.getAndIncrement()]);
                var p = info.gameInfo.getPlayer(id);
                if (p != null) {
                    if (p.id == info.gameInfo.player.id && !info.gameInfo.resigned) {
                        System.out.println("ERROR how the hell did i get my own id??");
                        return FAIL;
                    }

                    int readyTime = Integer.parseInt(input[index.getAndIncrement()]);
                    if (readyTime > p.readyTime) {
                        p.readyTime = readyTime;
                        p.ready = Byte.parseByte(input[index.getAndIncrement()]);
                    } else index.getAndIncrement();
                    int inTheRaceTime = Integer.parseInt(input[index.getAndIncrement()]);
                    if (inTheRaceTime > p.inTheRaceTime) {
                        p.inTheRaceTime = inTheRaceTime;
                        p.inTheRace = Byte.parseByte(input[index.getAndIncrement()]) != 0;
                    }
                } else
                    System.out.println("ERROR Could not find player with id: " + id);
            }
            case tempStatus -> { // send med tidsreferanse.
            	long gmTime = Long.parseLong(input[index.getAndIncrement()]);
            	boolean isRacing = Integer.parseInt(input[index.getAndIncrement()]) != 0;
            	int round = Integer.parseInt(input[index.getAndIncrement()]);
            	if (info.gameInfo.gmCreationTime > gmTime) {
                    // disregard, throw in trash
                    return ResponseState.FAIL;
                } else if (info.gameInfo.gmCreationTime < gmTime) {
                    // store for later
                    System.err.println("info gm time " + info.gameInfo.gmCreationTime + " != " + gmTime);
                    return KEEP_BACK;
                }
                // Dont set gmTime
                info.gameInfo.getGamemode().setRacing(isRacing);
                info.gameInfo.getGamemode().setRound(round);

                long countdown = Long.parseLong(input[index.getAndIncrement()]);
                long timeRef = Long.parseLong(input[index.getAndIncrement()]);
                countdown += System.currentTimeMillis() - timeRef;

                if (info.gameInfo.isGameStarted())
                    info.gameInfo.setCountdown(countdown);

                while (index.get() < input.length) {
                    var rawPlayerSplit = input[index.getAndIncrement()].split(specialSplit);
                    var id = Byte.parseByte(rawPlayerSplit[0]);
                    var p = info.gameInfo.getPlayer(id);
                    if (p != null) {
                        if (p.id == info.gameInfo.player.id) continue;

                        int readyTime = Integer.parseInt(rawPlayerSplit[1]);
                        if (readyTime > p.readyTime) {
                            p.readyTime = readyTime;
                            p.ready = Byte.parseByte(rawPlayerSplit[2]);
                        }
                        int inTheRaceTime = Integer.parseInt(rawPlayerSplit[3]);
                        if (inTheRaceTime > p.inTheRaceTime) {
                            p.inTheRaceTime = inTheRaceTime;
                            p.inTheRace = Byte.parseByte(rawPlayerSplit[4]) != 0;
                        }
                    } else
                        System.out.println("ERROR Could not find player with id: " + id);
                }
            }
            case raceLights -> info.gameInfo.setRaceLightsString(input, index);
            case chat ->
                    info.gameInfo.addChatFromServer(message.requestMessage.substring(message.requestMessage.indexOf("#") + 1));
            case beep -> {
                var player = getPlayer(info, input, index);
                if (player != null)
                    player.car.beep();

            }
        }

        System.out.println((System.currentTimeMillis() - fromTime) + " ms resp");

        return ResponseState.ALL_GOOD;
    }

//    public void setAfterJoined(Consumer<Player> afterJoined) {
//        if (afterJoined != null)
//            this.afterJoined = afterJoined;
//        else
//            System.out.println("afterjoin tried to set null");
//    }

    public static String getCloneString(ICloneStringable clone) {
        var sb = new StringBuilder();
        clone.getCloneString(sb, 0, Translator.split, false);
        return sb.toString();
    }

    public static void setCloneString(ICloneStringable clone, String cloneString) {
        clone.setCloneString(cloneString.split(Translator.split), new AtomicInteger());
    }

    public static ICloneStringable setCloneString(ICloneStringable clone, ICloneStringable original) {
        setCloneString(clone, getCloneString(original));
        return clone;
    }

    public static String initials(String indicator, Player player) {
        return indicator + split + player.id + split + player.gameID + split;
    }

//    public boolean checkBeforeJoined(Message message) {
//        if (afterJoined != null && !message.requestMessage.substring(resAll.length()).startsWith(join)) {
//            afterJoinedEarlyCalls.offer(message);
//            return true;
//        }
//        return false;
//    }


}
