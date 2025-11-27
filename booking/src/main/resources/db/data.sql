-- Reference data. Each insert is guarded so the script can be replayed.

insert into carriers (code, name, active, base_transit_hours, reliability, max_weight_kg)
select 'NSL', 'NorthStar Lines', true, 96, 0.940, 24000.00
where not exists (select 1 from carriers where code = 'NSL');

insert into carriers (code, name, active, base_transit_hours, reliability, max_weight_kg)
select 'BLX', 'Baltic Express', true, 72, 0.880, 18000.00
where not exists (select 1 from carriers where code = 'BLX');

insert into carriers (code, name, active, base_transit_hours, reliability, max_weight_kg)
select 'PCR', 'Pacific Reach', true, 168, 0.810, 30000.00
where not exists (select 1 from carriers where code = 'PCR');
