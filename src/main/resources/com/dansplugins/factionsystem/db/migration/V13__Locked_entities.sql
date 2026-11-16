-- Locks on armour stands and item frames (#1679). Additive: two new tables, nothing existing is
-- changed. As with mf_locked_block, a lock is removed with the claim it was made in and with its
-- owner's player record.
create table `mf_locked_entity`(
    `id` varchar(36) primary key not null,
    `version` integer not null,
    `entity_id` varchar(36) not null unique,
    `world_id` varchar(36) not null,
    `chunk_x` integer not null,
    `chunk_z` integer not null,
    `player_id` varchar(36) not null,
    foreign key(`world_id`, `chunk_x`, `chunk_z`) references `mf_claimed_chunk`(`world_id`, `x`, `z`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);

create table `mf_locked_entity_accessor`(
    `locked_entity_id` varchar(36) not null,
    `player_id` varchar(36) not null,
    primary key(`locked_entity_id`, `player_id`),
    foreign key(`locked_entity_id`) references `mf_locked_entity`(`id`) on delete cascade,
    foreign key(`player_id`) references `mf_player`(`id`) on delete cascade
);
