alter table `mf_player`
    add `power_at_logout` double null;

update `mf_player` set `power_at_logout` = `power`;

alter table `mf_player`
    modify `power_at_logout` double not null;