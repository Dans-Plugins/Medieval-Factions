-- Roles of the lock owner's faction that may access a locked block (#1569). Additive: no existing
-- table or row is changed. Roles are stored inside mf_faction.roles as JSON, so role_id has no
-- foreign key; a row whose role has since been deleted grants nothing.
create table `mf_locked_block_role_accessor`(
    `locked_block_id` varchar(36) not null,
    `role_id` varchar(36) not null,
    primary key(`locked_block_id`, `role_id`),
    foreign key(`locked_block_id`) references `mf_locked_block`(`id`) on delete cascade
);
