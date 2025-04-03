package communication.remote;

import comNew.ConnectedLine;
import communication.Translator;
import game_modes.GameMode;
import main.Main;
import player_local.Player;
import player_local.car.Rep;

public class Message {
    public long requesterID;
    public String requestMessage;
    public final ConnectedLine connectedLine;

    public static final byte REQUESTER_ID_ALL = Byte.MIN_VALUE;

    public Message(long requesterID, String requestMessage, ConnectedLine connectedLine) {
        this.requesterID = requesterID;
        this.requestMessage = requestMessage.trim();
        this.connectedLine = connectedLine;
    }

    public Message(long requesterID, String requestMessage) {
        this(requesterID, requestMessage, null);
    }

    @Override
    public String toString() {
        return "id: " + requesterID + ", message: " + requestMessage;
    }

    public static Message msgJoin(long host, Player player) {
        return new Message(host,
                Translator.mailRequest + Translator.join + Translator.split
                        + Main.VERSION + Translator.split
                        + player.name.replaceAll("#", Translator.hashtag) + Translator.split
                        + player.role + Translator.split
                        + player.steamID);
    }

    public static String msgJoinTell(Player player, int endGoal) {
        return Translator.mailRes + Translator.joinTell + Translator.split
                + Translator.getCloneString(player) + Translator.split
                + endGoal;
    }

    public static String msgJoined(Player player) {
        return Translator.mailRes + Translator.initials(Translator.joined, player);
    }

    public static Message msgLeave(Player player) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.leave, player));
    }

    public static Message msgKick(Player player) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.kick, player));
    }

    public static Message msgRaceStarted(int round) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.raceStarted + Translator.split + round);
    }

    public static Message msgReady(Player player, int readyTime, byte ready) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.ready, player)
                + readyTime + Translator.split
                + ready);
    }

    public static Message msgFinishRace(Player player, long time, int speed) {
        return new Message(REQUESTER_ID_ALL,
                Translator.mailResAll + Translator.initials(Translator.finish, player)
                        + time + Translator.split
                        + speed + Translator.split
                        + player.getCarRep().getInt(Rep.highestSpdAchived));
    }

    public static Message msgSelectCar(Player player, int carSelectionTime, int selectedCarIndex, boolean random) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.selectedCarIndex, player)
                + carSelectionTime + Translator.split
                + selectedCarIndex + Translator.split
                + (random ? 1 : 0));
    }

    public static Message msgUpdateClone(Player player, String cloneString, int historyIndex) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.updateClone, player)
                + historyIndex
                + Translator.split
                + Translator.specialSplit
                + cloneString );
    }

    public static Message msgUndo(Player player, int index, int undoTime, int round, boolean redo) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.undo, player)
                + (redo ? 1 : 0) + Translator.split
                + undoTime + Translator.split
                + index  + Translator.split
                + round);
    }

    public static Message msgSetInRace(Player player, boolean in) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.inTheRace, player) + (in ? 1 : 0));
    }

    public static Message msgRaceLights(String racelights) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.raceLights + Translator.split + racelights);
    }

    public static Message msgTempStatus(long gmTime, GameMode gm, long countdown, long now, Player[] players) {
        var readys = new StringBuilder();
        for (var p : players)
            readys.append(Translator.split)
                    .append(p.id).append(Translator.specialSplit)
                    .append(p.readyTime).append(Translator.specialSplit)
                    .append(p.ready).append(Translator.specialSplit)
                    .append(p.inTheRaceTime).append(Translator.specialSplit)
                    .append(p.inTheRace ? 1 : 0);

        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.tempStatus + Translator.split
        		+ gmTime + Translator.split
        		+ (gm.isRacing() ? 1 : 0) + Translator.split
        		+ gm.getRound() + Translator.split
                + countdown + Translator.split
                + now
                + readys
        );
    }

    public static Message msgMyPlayerStatus(Player p) {
        return new Message(REQUESTER_ID_ALL,
                Translator.mailResAll + Translator.myStatus + Translator.split
                        + p.id + Translator.split +
                        p.readyTime + Translator.split +
                        p.ready + Translator.split +
                        p.inTheRaceTime + Translator.split +
                        (p.inTheRace ? 1 : 0));
    }

    public static Message msgGameModeChange(GameMode gm, long gmTime) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.changeGamemode + Translator.split
                + gmTime + Translator.split
                + gm.getAllInfo());
    }

    public static Message msgRaceInfo(Player player, float distance, int speed, double spdinc, boolean brake, long raceTime) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.raceInformation, player)
                + distance + Translator.split
                + speed + Translator.split
                + spdinc + Translator.split
                + (brake ? 1 : 0) + Translator.split
                + raceTime);
    }

    public static Message msgGameModeAllInfo(Player player, GameMode gameMode, long raceSeed) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.afterRaceGamemodeUpdate, player)
        		+ raceSeed + Translator.split
                + gameMode.getGameModeEnum() + Translator.split
                + gameMode.getAllInfo());
    }

    public static Message msgChat(String chatMsg) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.chat + Translator.split + chatMsg);
    }

    public static Message msgResign(Player player) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.resign, player));
    }

    public static Message msgBeep(Player player) {
        return new Message(REQUESTER_ID_ALL, Translator.mailResAll + Translator.initials(Translator.beep, player));
    }

}
