INSERT INTO dev.users (id, email, first_name, last_name, password, enabled)
VALUES (1, 'craig@gmail.com', 'Craig', 'Miller', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true),
       (2, 'bob@gmail.com', 'Bob', 'Saget', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true),
       (3, 'auth@gmail.com', 'Auth', 'Code', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true);
SELECT setval('dev.users_id_seq', 3, true);

INSERT INTO dev.clients (id, name, client_key, client_secret, enabled, access_token_timeout_secs, refresh_token_timeout_secs, auth_code_timeout_secs)
VALUES (1, 'ClientCredsApp', 'client_creds', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true, 300, 3600, 60),
       (2, 'PasswordApp', 'password', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true, 300, 3600, 60),
       (3, 'AuthCodeApp', 'auth_code', '{bcrypt}$2a$10$HYKpEK6BFUFH99fHm5yOhuk4hn1gFErtLveeonVSHW1G7n5bUhGUe', true, 300, 3600, 60),
       (4, 'auth-management-service', 'a4cc4fef-564e-44c1-82af-45572f124c1a', '{bcrypt}$2a$10$Mo7pB5wHzuChfanS1c9vOOKRmdn0.TEWDi43yjd6jstdkHtmT/FXa', true, 300, 3600, 60);
SELECT setval('dev.clients_id_seq', 4, true);

INSERT INTO dev.client_redirect_uris (client_id, redirect_uri)
values (3, 'http://somewhere.com'),
       (4, 'https://localhost:3000/api/oauth/authcode/code'),
       (4, 'https://192.168.4.27:3000/api/oauth/authcode/code');

INSERT INTO dev.roles (id, name, client_id)
VALUES (1, 'ROLE_READ', 2),
       (2, 'ROLE_WRITE', 2);
SELECT setval('dev.roles_id_seq', 2, true);

INSERT INTO dev.client_users (id, user_id, client_id)
VALUES (1, 1, 2), (2, 3, 3), (3, 1, 4), (4, 2, 2);
SELECT setval('dev.client_users_id_seq', 4, true);

INSERT INTO dev.client_user_roles (user_id, client_id, role_id)
VALUES (1, 2, 1), (1, 2, 2);
SELECT setval('dev.client_user_roles_id_seq', 2, true);
