package dansplugins.factionsystem.commands.abs;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalChunkService;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 * @since 05/05/2021 - 12:18
 */
public abstract class SubCommand implements ColorTranslator {

    // Constants
    public static final String LOCALE_PREFIX = "Locale_";

    // Data classes
    protected final LocalLocaleService locale;
    protected final PersistentData data;
    protected final ArgumentParser parser;
    protected final EphemeralData ephemeral;
    protected final LocalChunkService chunks;
    protected final DynmapIntegrator dynmap;
    protected final LocalConfigService localConfigService;

    // Command Data
    private final String[] names;
    private final boolean playerCommand;
    private final boolean requiresFaction;
    private final boolean requiresOfficer;
    private final boolean requiresOwner;

    // Player data
    protected Faction faction = null;

    /**
     * Constructor to initialise a Command.
     * @param names of the command, for example, "Fly, FFly, Flight".
     * @param playerCommand if the command is exclusive to players.
     * @param requiresFaction if the command requires a Faction to perform.
     * @param requiresOfficer if the command requires officer or higher.
     * @param requiresOwner if the command is reserved for Owners.
     */
    public SubCommand(String[] names,
                      boolean playerCommand,
                      boolean requiresFaction,
                      boolean requiresOfficer,
                      boolean requiresOwner) {
        // Local variables standing for instances of constantly used instances.
        this.locale = LocalLocaleService.getInstance();
        this.data = PersistentData.getInstance();
        this.parser = ArgumentParser.getInstance();
        this.ephemeral = EphemeralData.getInstance();
        this.chunks = LocalChunkService.getInstance();
        this.dynmap = DynmapIntegrator.getInstance();
        this.localConfigService = LocalConfigService.getInstance();

        // Load Command Names.
        this.names = new String[names.length];
        for (int i = 0; i < this.names.length; i++) {
            String name = names[i];
            if (name.contains(LOCALE_PREFIX)) name = locale.getText(name.replace(LOCALE_PREFIX, ""));
            this.names[i] = name;
        }

        this.playerCommand = playerCommand;
        this.requiresFaction = requiresFaction;
        this.requiresOfficer = requiresOfficer;
        this.requiresOwner = requiresOwner;
    }

    /**
     * Constructor to initialise a command without owner/faction checks.
     * @param names of the command.
     * @param playerCommand if the command is exclusive to players.
     * @param requiresFaction if the command requires a Faction to do.
     */
    public SubCommand(String[] names, boolean playerCommand, boolean requiresFaction) {
        this(names, playerCommand, requiresFaction, false, false);
    }

    /**
     * Constructor to initialise a command without faction checks.
     * @param names of the command.
     * @param playerCommand if the command is exclusive to players.
     */
    public SubCommand(String[] names, boolean playerCommand) {
        this(names, playerCommand, false);
    }

    /**
     * Method to be called by the command interpreter <em>only</em>.
     * <p>
     *     This method uses the in-class variables to call a different method based on the parameters specified.
     *     <br>For example, if {@link SubCommand#playerCommand} is {@code true},
     *     <br>{@link SubCommand#execute(Player, String[], String)} is executed,
     *     <br>not {@link SubCommand#execute(CommandSender, String[], String)}.
     * </p>
     * @param sender who sent the command.
     * @param args of the command.
     * @param key of the sub-command.
     */
    public void performCommand(CommandSender sender, String[] args, String key) {
        if (playerCommand) {
            if (!(sender instanceof Player)) { // Require a player for a player-only command.
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            Player player = (Player) sender;
            if (requiresFaction) { // Find and check the status of a Faction.
                this.faction = getPlayerFaction(player);
                if (faction == null) {
                    player.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                    return;
                }
                if (requiresOfficer) { // If the command requires an Officer or higher, check for it.
                    if (!(faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId()))) {
                        player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                        return;
                    }
                }
                if (requiresOwner) { // If the command requires an owner only, check for it.
                    if (!faction.isOwner(player.getUniqueId())) {
                        player.sendMessage(translate("&c" + getText("AlertMustBeOwnerToUseCommand")));
                        return;
                    }
                }
            }
            execute(player, args, key); // 100% a player so you can safely use it
            return;
        }
        execute(sender, args, key); // Sender can still be a player if this is executed.
    }

    /**
     * Method to execute the command for a player.
     * @param player who sent the command.
     * @param args of the command.
     * @param key of the sub-command (e.g. Ally).
     */
    public abstract void execute(Player player, String[] args, String key);

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
        return Arrays.stream(names).anyMatch(s -> s.equalsIgnoreCase(name));
    }

    /**
     * Method to check if a sender has a permission.
     * <p>
     *     If the sender doesn't have the permission, they are messaged the formatted no Permission message.
     * </p>
     * @param sender to check.
     * @param permission to test for.
     * @return {@code true} if they do.
     */
    public boolean checkPermissions(CommandSender sender, String... permission) {
        boolean has = false;
        for (String perm : permission) if (has = sender.hasPermission(perm)) break;
        if (!has) sender.sendMessage(translate("&c" + getText("PermissionNeeded", permission[0])));
        return has;
    }

    /**
     * Method to obtain text from a key.
     * @param key of the message in LocaleManager.
     * @return String message
     */
    protected String getText(String key) {
        return locale.getText(key);
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
            return data.getPlayersFaction(((OfflinePlayer) object).getUniqueId());
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

    /**
     * Method to send the entire Server a message.
     * @param message to send to the players.
     */
    protected void messageServer(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    /**
     * Method to get an Integer from a String.
     * @param line to convert into an Integer.
     * @param orElse if the conversion fails.
     * @return {@link Integer} numeric.
     */
    protected int getIntSafe(String line, int orElse) {
        try {
            return Integer.parseInt(line);
        } catch (Exception ex) {
            return orElse;
        }
    }

    /**
     * Method to test if something matches any goal string.
     * @param what to test
     * @param goals to compare with
     * @param matchCase for the comparison (or not)
     * @return {@code true} if something in goals matches what.
     */
    protected boolean safeEquals(boolean matchCase, String what, String... goals) {
        return Arrays.stream(goals).anyMatch(goal ->
                matchCase && goal.equals(what) || !matchCase && goal.equalsIgnoreCase(what)
        );
    }

    /**
     * Method to obtain the Config.yml for Medieval Factions.
     * @return {@link FileConfiguration}
     */
    protected FileConfiguration getConfig() {
        return MedievalFactions.getInstance().getConfig();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                    + "names=" + Arrays.toString(names)
                + '}';
    }

}
