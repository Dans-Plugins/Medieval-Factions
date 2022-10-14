/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands.abs;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
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
    public static final String LOCALE_PREFIX = "Locale_";
    protected final LocaleService localeService;
    protected final PersistentData persistentData;
    protected final EphemeralData ephemeralData;
    protected final PersistentData.ChunkDataAccessor chunkDataAccessor;
    protected final DynmapIntegrator dynmapIntegrator;
    protected final ConfigService configService;
    protected final PlayerService playerService;
    protected final MessageService messageService;
    private final boolean playerCommand;
    private final boolean requiresFaction;
    private final boolean requiresOfficer;
    private final boolean requiresOwner;
    protected Faction faction = null;
    protected String[] names;
    protected String[] requiredPermissions;

    /**
     * Constructor to initialise a Command.
     *
     * @param names             of the command, for example, "Fly, FFly, Flight".
     * @param playerCommand     if the command is exclusive to players.
     * @param requiresFaction   if the command requires a Faction to perform.
     * @param requiresOfficer   if the command requires officer or higher.
     * @param requiresOwner     if the command is reserved for Owners.
     * @param requiredPermissions permissions required to utilize this command
     * @param localeService
     * @param persistentData
     * @param ephemeralData
     * @param chunkDataAccessor
     * @param dynmapIntegrator
     * @param configService
     */
    public SubCommand(String[] names, boolean playerCommand, boolean requiresFaction, boolean requiresOfficer, boolean requiresOwner, String[] requiredPermissions, LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        this.localeService = localeService;
        this.persistentData = persistentData;
        this.ephemeralData = ephemeralData;
        this.chunkDataAccessor = chunkDataAccessor;
        this.dynmapIntegrator = dynmapIntegrator;
        this.configService = configService;
        loadCommandNames(names);
        this.playerCommand = playerCommand;
        this.requiresFaction = requiresFaction;
        this.requiresOfficer = requiresOfficer;
        this.requiresOwner = requiresOwner;
        this.playerService = playerService;
        this.messageService = messageService;
        this.requiredPermissions = requiredPermissions;
    }

    /**
     * Constructor to initialise a command without owner/faction checks.
     *
     * @param names               of the command.
     * @param playerCommand       if the command is exclusive to players.
     * @param requiresFaction     if the command requires a Faction to do.
     * @param requiredPermissions permissions required to utilize this command
     * @param persistentData
     * @param localeService
     * @param ephemeralData
     * @param configService
     * @param chunkDataAccessor
     * @param dynmapIntegrator
     */
    public SubCommand(String[] names, boolean playerCommand, boolean requiresFaction, String[] requiredPermissions, PersistentData persistentData, LocaleService localeService, EphemeralData ephemeralData, ConfigService configService, PlayerService playerService, MessageService messageService, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator) {
        this(names, playerCommand, requiresFaction, false, false, requiredPermissions, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
    }

    /**
     * Constructor to initialise a command without faction checks.
     *
     * @param names             of the command.
     * @param playerCommand     if the command is exclusive to players.
     * @param requiredPermissions permissions required to utilize this command
     * @param persistentData
     * @param localeService
     * @param ephemeralData
     * @param configService
     * @param chunkDataAccessor
     * @param dynmapIntegrator
     */
    public SubCommand(String[] names, boolean playerCommand, String[] requiredPermissions, PersistentData persistentData, LocaleService localeService, EphemeralData ephemeralData, ConfigService configService, PlayerService playerService, MessageService messageService, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator) {
        this(names, playerCommand, false, persistentData, requiredPermissions, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
    }

    protected void loadCommandNames(String[] names) {
        this.names = new String[names.length];
        for (int i = 0; i < this.names.length; i++) {
            String name = names[i];
            if (name.contains(LOCALE_PREFIX)) name = this.localeService.getText(name.replace(LOCALE_PREFIX, ""));
            this.names[i] = name;
        }
    }

    /**
     * Method to be called by the command interpreter <em>only</em>.
     * <p>
     * This method uses the in-class variables to call a different method based on the parameters specified.
     * <br>For example, if {@link SubCommand#playerCommand} is {@code true},
     * <br>{@link SubCommand#execute(Player, String[], String)} is executed,
     * <br>not {@link SubCommand#execute(CommandSender, String[], String)}.
     * </p>
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command.
     */
    public void performCommand(CommandSender sender, String[] args, String key) {
        if (this.playerCommand) {
            if (!(sender instanceof Player)) { // Require a player for a player-only command.
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            Player player = (Player) sender;
            if (this.requiresFaction) { // Find and check the status of a Faction.
                this.faction = this.getPlayerFaction(player);
                if (this.faction == null) {
                    player.sendMessage(translate("&c" + getText("AlertMustBeInFactionToUseCommand")));
                    return;
                }
                if (this.requiresOfficer) { // If the command requires an Officer or higher, check for it.
                    if (!(faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId()))) {
                        player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                        return;
                    }
                }
                if (this.requiresOwner && !faction.isOwner(player.getUniqueId())) { // If the command requires an owner only, check for it.
                    player.sendMessage(translate("&c" + getText("AlertMustBeOwnerToUseCommand")));
                    return;
                }
            }
            if (!this.checkPermissions(sender, true)) {
                return;
            }
            this.execute(player, args, key); // 100% a player so you can safely use it
            return;
        }
        this.execute(sender, args, key); // Sender can still be a player if this is executed.
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    public abstract void execute(Player player, String[] args, String key);

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    public abstract void execute(CommandSender sender, String[] args, String key);

    /**
     * Parent method to conduct tab completion. This will check permissions first, then hand to the child.
     */
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (!this.checkPermissions(sender)) return null;
        return this.handleTabComplete(sender, args);
    }

    /**
     * Child method to conduct tab completion. Classes that inherit this class should override this if they can offer tab completion.
     */
    public List<String> handleTabComplete(CommandSender sender, String[] args) {
        return null;
    }


    /**
     * Method to determine if a String is this SubCommand or not.
     *
     * @param name of the command.
     * @return {@code true} if it is.
     */
    public boolean isCommand(String name) {
        return Arrays.stream(this.names).anyMatch(s -> s.equalsIgnoreCase(name));
    }

    // Helper methods for checkPermissions in different cases
    public boolean checkPermissions(CommandSender sender) {
        return this.checkPermissions(sender, false, ...this.requiredPermissions);
    }

    public boolean checkPermissions(CommandSender sender, boolean announcePermissionsMissing) {
        return this.checkPermissions(sender, announcePermissionsMissing, ...this.requiredPermissions);
    }

    public boolean checkPermissions(CommandSender sender, String... permissions) {
        return this.checkPermissions(sender, true, permissions);
    }

    /**
     * Method to check if a sender has a permission.
     * <p>
     * If the sender doesn't have the permission, they are messaged the formatted no Permission message.
     * </p>
     *
     * @param sender     to check.
     * @param permission to test for.
     * @return {@code true} if they do.
     */
    public boolean checkPermissions(CommandSender sender, boolean announcePermissionsMissing, String... permissions) {
        boolean hasPermission = false;
        String[] missingPermissions = [];
        for (String perm : permissions) {
            hasPermission = sender.hasPermission(perm);
            if (hasPermission) break;
            missingPermissions.append(perm);
        }
        if (!hasPermission && announcePermissionsMissing) {
            playerService.sendMessage(sender, translate("&c" + getText("PermissionNeeded", missingPermissions.join(', '))), Objects.requireNonNull(this.messageService.getLanguage().getString("PermissionNeeded")).replace("#permission#", missingPermissions.join(' ')), true);
        }
        return hasPermission;
    }

    /**
     * Method to obtain text from a key.
     *
     * @param key of the message in LocaleManager.
     * @return String message
     */
    protected String getText(String key) {
        String text = this.localeService.getText(key);
        return text.replace("%d", "%s");
    }

    /**
     * Method to obtain text from a key with replacements.
     *
     * @param key          to obtain.
     * @param replacements to replace within the message using {@link String#format(String, Object...)}.
     * @return String message
     */
    protected String getText(String key, Object... replacements) {
        return String.format(this.getText(key), replacements);
    }

    /**
     * Method to obtain a Player faction from an object.
     * <p>
     * This method can accept a UUID, Player, OfflinePlayer and a String (name or UUID).<br>
     * If the type isn't found, an exception is thrown.
     * </p>
     *
     * @param object to obtain the Player faction from.
     * @return {@link Faction}
     * @throws IllegalArgumentException when the object isn't compatible.
     */
    @SuppressWarnings("deprecation")
    protected Faction getPlayerFaction(Object object) {
        if (object instanceof OfflinePlayer) {
            return this.persistentData.getPlayersFaction(((OfflinePlayer) object).getUniqueId());
        } else if (object instanceof UUID) {
            return this.persistentData.getPlayersFaction((UUID) object);
        } else if (object instanceof String) {
            try {
                return persistentData.getPlayersFaction(UUID.fromString((String) object));
            } catch (Exception e) {
                OfflinePlayer player = Bukkit.getOfflinePlayer((String) object);
                if (player.hasPlayedBefore()) {
                    return this.persistentData.getPlayersFaction(player.getUniqueId());
                }
            }
        }
        throw new IllegalArgumentException(object + " cannot be transferred into a Player");
    }

    /** 
     * Method to retrieve the list of command names for this command.
     */
    public String[] getCommandNames() {
        return this.names;
    }

    /**
     * Get primary command name.
    */
    public String getPrimaryCommandName() {
        return this.names[0];
    }

    /**
     * Method to obtain a Faction by name.
     * <p>
     * This is a passthrough function.
     * </p>
     *
     * @param name of the desired Faction.
     * @return {@link Faction}
     */
    protected Faction getFaction(String name) {
        return this.persistentData.getFaction(name);
    }

    /**
     * Method to send an entire Faction a message.
     *
     * @param faction    to send a message to.
     * @param oldMessage old message to send to the Faction.
     * @param newMessage new message to send to the Faction.
     */
    protected void messageFaction(Faction faction, String oldMessage, String newMessage) {
        faction.getMemberList()
            .stream()
            .map(Bukkit::getOfflinePlayer)
            .filter(OfflinePlayer::isOnline)
            .map(OfflinePlayer::getPlayer)
            .filter(Objects::nonNull)
            .forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    /**
     * Method to send the entire Server a message.
     *
     * @param oldMessage old message to send to the players.
     * @param newMessage old message to send to the players.
     */
    protected void messageServer(String oldMessage, String newMessage) {
        Bukkit.getOnlinePlayers().forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    /**
     * Method to get an Integer from a String.
     *
     * @param line   to convert into an Integer.
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
     *
     * @param what  to test
     * @param goals to compare with
     * @return {@code true} if something in goals matches what.
     */
    protected boolean safeEquals(String what, String... goals) {
        return Arrays.stream(goals).anyMatch(goal -> goal.equalsIgnoreCase(what));
    }

    /**
     * Method to obtain the Config.yml for Medieval Factions.
     *
     * @return {@link FileConfiguration}
     */
    protected FileConfiguration getConfig() {
        return this.configService.getConfig();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + "names=" + Arrays.toString(this.names) + '}';
    }

}
