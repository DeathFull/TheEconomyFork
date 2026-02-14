package com.economy.economy;

import java.util.HashMap;
import java.util.UUID;

public class BalanceTracker {

    private HashMap<UUID, PlayerBalance> balances;

    public BalanceTracker() {
        this.balances = new HashMap<>();
    }

    public PlayerBalance[] getBalances() {
        return balances.values().toArray(new PlayerBalance[0]);
    }

    public void setBalances(PlayerBalance[] balances) {
        this.balances = new HashMap<>();
        for (PlayerBalance balance : balances) {
            this.balances.put(balance.getUuid(), balance);
        }
    }

    public double getBalance(UUID uuid) {
        if (balances.containsKey(uuid)) {
            return balances.get(uuid).getBalance();
        }
        return 0.0;
    }

    public void setBalance(UUID uuid, double balance) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, new PlayerBalance(uuid, balance));
        } else {
            balances.get(uuid).setBalance(balance);
        }
    }

    public void setPlayerNick(UUID uuid, String nick) {
        if (balances.containsKey(uuid)) {
            balances.get(uuid).setNick(nick);
        }
    }

    public String getPlayerNick(UUID uuid) {
        if (balances.containsKey(uuid)) {
            String nick = balances.get(uuid).getNick();
            return (nick != null && !nick.isEmpty()) ? nick : null;
        }
        return null;
    }

    public void addBalance(UUID uuid, double amount) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, new PlayerBalance(uuid, amount));
        } else {
            balances.get(uuid).addBalance(amount);
        }
    }

    public void setBalanceWithNick(UUID uuid, String nick, double balance) {
        if (!balances.containsKey(uuid)) {
            balances.put(uuid, new PlayerBalance(uuid, nick, balance));
        } else {
            PlayerBalance playerBalance = balances.get(uuid);
            playerBalance.setNick(nick);
            playerBalance.setBalance(balance);
        }
    }

    public boolean subtractBalance(UUID uuid, double amount) {
        if (!balances.containsKey(uuid)) {
            return false;
        }
        PlayerBalance balance = balances.get(uuid);
        if (balance.getBalance() < amount) {
            return false;
        }
        balance.subtractBalance(amount);
        return true;
    }

    public boolean hasBalance(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public int getCash(UUID uuid) {
        if (balances.containsKey(uuid)) {
            return balances.get(uuid).getCash();
        }
        return 0;
    }

    public void setCash(UUID uuid, int cash) {
        if (!balances.containsKey(uuid)) {
            PlayerBalance balance = new PlayerBalance(uuid, 0.0);
            balance.setCash(cash);
            balances.put(uuid, balance);
        } else {
            balances.get(uuid).setCash(cash);
        }
    }

    public void addCash(UUID uuid, int amount) {
        if (!balances.containsKey(uuid)) {
            PlayerBalance balance = new PlayerBalance(uuid, 0.0);
            balance.setCash(amount);
            balances.put(uuid, balance);
        } else {
            balances.get(uuid).addCash(amount);
        }
    }

    public boolean subtractCash(UUID uuid, int amount) {
        if (!balances.containsKey(uuid)) {
            return false;
        }
        PlayerBalance balance = balances.get(uuid);
        if (balance.getCash() < amount) {
            return false;
        }
        balance.subtractCash(amount);
        return true;
    }

    public boolean hasCash(UUID uuid, int amount) {
        return getCash(uuid) >= amount;
    }
}


