-- Fixed player power (#1682): an admin can pin a player's power to a set amount with /power setfixed.
-- Additive: one nullable column. NULL (every existing row) means the player's power changes normally.
alter table `mf_player`
    add `fixed_power` double null;
