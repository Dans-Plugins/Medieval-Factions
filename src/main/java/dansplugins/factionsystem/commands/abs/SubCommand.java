package dansplugins.factionsystem.commands.abs;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 * @since 05/05/2021 - 12:18
 */
public abstract class SubCommand implements ColorTranslator {

    private final String[] names;
    private final LocaleManager manager;
    private final PersistentData data;
    protected final ArgumentParser parser;

    /**
     * Constructor to initialise a Command.
     * @param names of the command, for example, "Fly, FFly, Flight".
     */
    public SubCommand(String[] names) {
        // Local variables standing for instances of constantly used instances.
        this.manager = LocaleManager.getInstance();
        this.data = PersistentData.getInstance();
        this.parser = ArgumentParser.getInstance();

        // Load Command Names.
        this.names = new String[names.length];
        for (int i = 0; i < this.names.length; i++) {
            String name = names[i];
            if (name.contains("Locale_")) name = manager.getText(name.replace("Locale_", ""));
            this.names[i] = name;
        }
    }

    /**
     * Method to execute the command.
     * @param sender who sent the command.
     * @param args of the command.
     * @param key of the command.
     */
    public abstract void execute(CommandSender sender, String[] args, String key);

    /**
     * Method to determine if a String is this SubCommand or not.
     * @param name of the command.
     * @return {@code true} if it is.
     */
    public boolean isCommand(String name) {
        return Arrays.stream(names)
                .anyMatch(s -> s.equalsIgnoreCase(name) || getClass().getSimpleName().equalsIgnoreCase(name));
    }

    /**
     * Method to determine if a CommandSender is a Player.
     * @param sender to test.
     * @return {@code true} if they are.
     */
    protected boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Method to obtain text from a key.
     * @param key of the message in LocaleManager.
     * @return String message
     */
    protected String getText(String key) {
        return manager.getText(key);
    }

    /**
     * Method to obtain text from a key with replacements.
     * @param key to obtain.
     * @param replacements to replace within the message using {@link String#format(String, Object...)}.
     * @return String message
     */
    protected String getText(String key, Object... replacements) {
        return String.format(getText(key), replacements);
    }

    /**
     * Method to obtain a Player faction from an object.
     * <p>
     *     This method can accept a UUID, Player, OfflinePlayer and a String (name or UUID).<br>
     *     If the type isn't found, an exception is thrown.
     * </p>
     * @param object to obtain the Player faction from.
     * @return {@link Faction}
     * @throws IllegalArgumentException when the object isn't compatible.
     */
    @SuppressWarnings("deprecation")
    protected Faction getPlayerFaction(Object object) {
        if (object instanceof OfflinePlayer) {
            return data.getPlayersFaction(((OfflinePlayer)object).getUniqueId());
        } else if (object instanceof UUID) {
            return data.getPlayersFaction((UUID) object);
        } else if (object instanceof String) {
            try {
                return data.getPlayersFaction(UUID.fromString((String) object));
            } catch (Exception ex) {
                OfflinePlayer player = Bukkit.getOfflinePlayer((String) object);
                if (player.hasPlayedBefore()) {
                    return data.getPlayersFaction(player.getUniqueId());
                }
            }
        }
        throw new IllegalArgumentException(object + " cannot be transferred into a Player");
    }

    /**
     * Method to obtain a Faction by name.
     * <p>
     *     This is a passthrough function.
     * </p>
     * @param name of the desired Faction.
     * @return {@link Faction}
     */
    protected Faction getFaction(String name) {
        return data.getFaction(name);
    }

    /**
     * Method to send an entire Faction a message.
     * @param faction to send a message to.
     * @param message to send to the Faction.
     */
    protected void messageFaction(Faction faction, String message) {
        faction.getMemberList().stream()
                .map(Bukkit::getOfflinePlayer)
                .filter(OfflinePlayer::isOnline)
                .map(OfflinePlayer::getPlayer)
                .filter(Objects::nonNull)
                .forEach(player -> player.sendMessage(message));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                    + "names=" + Arrays.toString(names)
                + '}';
    }

}
