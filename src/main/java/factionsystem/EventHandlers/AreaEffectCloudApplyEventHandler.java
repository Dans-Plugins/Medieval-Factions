package factionsystem.EventHandlers;

import factionsystem.MedievalFactions;
import factionsystem.Util.Pair;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

import java.util.ArrayList;
import java.util.List;


public class AreaEffectCloudApplyEventHandler implements Listener {

        @EventHandler()
        public void handle(AreaEffectCloudApplyEvent event) {
            AreaEffectCloud cloud = event.getEntity();

            if (MedievalFactions.getInstance().utilities.potionTypeBad(cloud.getBasePotionData().getType())){
                // Search to see if cloud is in the stored list in MedievalFactions.getInstance()
                for (Pair<Player, AreaEffectCloud> storedCloudPair : MedievalFactions.getInstance().activeAOEClouds){
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
                                    if (MedievalFactions.getInstance().utilities.arePlayersInAFaction(attacker, potentialVictim) &&
                                            (MedievalFactions.getInstance().utilities.arePlayersFactionsNotEnemies(attacker, potentialVictim) ||
                                                    MedievalFactions.getInstance().utilities.arePlayersInSameFaction(attacker, potentialVictim))) {
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
