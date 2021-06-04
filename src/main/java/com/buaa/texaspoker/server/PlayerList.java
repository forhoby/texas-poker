package com.buaa.texaspoker.server;

import com.buaa.texaspoker.entity.player.Player;
import com.buaa.texaspoker.entity.player.ServerPlayer;
import com.buaa.texaspoker.network.IPacket;
import com.buaa.texaspoker.network.IPlayerPacketFactory;
import com.buaa.texaspoker.network.NetworkManager;
import com.buaa.texaspoker.network.login.SPacketPlayerCreate;
import com.buaa.texaspoker.network.play.SPacketPlayerJoin;
import com.buaa.texaspoker.network.play.IServerPlayHandler;
import com.buaa.texaspoker.network.play.SPacketPlayerLeave;
import com.buaa.texaspoker.network.play.ServerPlayHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PlayerList {

    private static final Logger logger = LogManager.getLogger();

    private GameServer server;
    private int maxPlayer;
    private List<Player> players = new LinkedList<>();

    public PlayerList(GameServer server, int maxPlayer) {
        this.server = server;
        this.maxPlayer = maxPlayer;
    }

    public void addPlayer(NetworkManager networkManager, ServerPlayer player) {
        logger.info("{} {} join the game !", player.getName(), networkManager.getSocketAddress());

        player.networkManager = networkManager;
        players.add(player);
        IServerPlayHandler serverPlayHandler = new ServerPlayHandler(server, networkManager, player);
        networkManager.setHandler(serverPlayHandler);
        networkManager.sendPacket(new SPacketPlayerCreate(player.getUuid(), player.getName()));
        this.sendToAll(new SPacketPlayerJoin(this.players));
    }

    public void removePlayer(ServerPlayer player) {
        players.removeIf(player1 -> player1.getUuid().equals(player.getUuid()));
        this.sendToAll(new SPacketPlayerLeave(this.players));
    }

    public void sendToAll(IPacket<?> iPacket) {
        this.players.forEach(player -> player.networkManager.sendPacket(iPacket));
    }

    public void sendToAll(IPlayerPacketFactory packetFactory) {
        this.players.forEach(player -> player.networkManager.sendPacket(packetFactory.create(player)));
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Player> getAlivePlayers() {
        return players.stream().filter(player -> !player.isOut()).collect(Collectors.toList());
    }

    public List<Player> getCalculatedPlayers() {
        return this.getPlayers().stream()
                .filter(player1 -> !player1.getData().isGiveUp() && !player1.isOut())
                .collect(Collectors.toList());
    }

    public List<Player> getAvailablePlayers() {
        return this.getPlayers().stream()
                .filter(player1 -> player1.getMoney() > 0 && !player1.getData().isGiveUp() && !player1.isOut())
                .collect(Collectors.toList());
    }
}
