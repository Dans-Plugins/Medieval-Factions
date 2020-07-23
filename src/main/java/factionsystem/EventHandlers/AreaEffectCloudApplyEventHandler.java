package factionsystem.EventHandlers;

import factionsystem.Main;
import factionsystem.Util.Pair;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import java.util.ArrayList;
import java.util.List;


public class AreaEffectCloudApplyEventHandler {

        Main main = null;

        public AreaEffectCloudApplyEventHandler(Main plugin) {
            main = plugin;
        }

        public void handle(AreaEffectCloudApplyEvent event) {
            AreaEffectCloud cloud = event.getEntity();

            if (main.utilities.potionTypeBad(cloud.getBasePotionData().getType())){
                // Search to see if cloud is in the stored list in main
                for (Pair<Player, AreaEffectCloud> storedCloudPair : main.activeAOEClouds){
                    if (storedCloudPair.getRight() == cloud){
                        //Check player is not allied with effected entities if any allied remove entity from list.
                        Player attacker = storedCloudPair.getLeft();

                        List<Player> alliedVictims = new ArrayList<>();
                        for (Entity potentialVictimEntity : event.getAffectedEntities()){
                            if (potentialVictimEntity instanceof Player){
                                Player potentialVictim = (Player) potentialVictimEntity;

                                    if (attacker == potentialVictim){
                                        continue;
                                    }

                                    // If both are in a faction and not at war.
                                    if (main.utilities.arePlayersInAFaction(attacker, potentialVictim) &&
                                            (main.utilities.arePlayersFactionsNotEnemies(attacker, potentialVictim) ||
                                                    main.utilities.arePlayersInSameFaction(attacker, potentialVictim))) {
                                        alliedVictims.add(potentialVictim);
                                    }
                                }
                            }

                        // Remove attacker's allies from the list
                        event.getAffectedEntities().removeAll(alliedVictims);
                        }
                    }
                }
            }
        }
