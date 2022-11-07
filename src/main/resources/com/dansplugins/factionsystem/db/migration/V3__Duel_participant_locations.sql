alter table `mf_duel`
    add `challenger_world` varchar(36) null,
    add `challenger_x` double null,
    add `challenger_y` double null,
    add `challenger_z` double null,
    add `challenger_yaw` real null,
    add `challenger_pitch` real null,
    add `challenged_world` varchar(36) null,
    add `challenged_x` double null,
    add `challenged_y` double null,
    add `challenged_z` double null,
    add `challenged_yaw` real null,
    add `challenged_pitch` real null;