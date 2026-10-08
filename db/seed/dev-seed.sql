-- =====================================================================
-- Development seed data for LibraryMS. NOT a Flyway migration: it is run
-- by hand against a local database after Flyway has created the schema.
--
--   docker exec -i libraryms-db-1 sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1' < db/seed/dev-seed.sql
--
-- Safe to re-run: it does nothing if any book already exists.
-- Loan dates are relative to now(), so overdue loans stay overdue.
--
-- Seeded reader accounts (local development only):
--   amara / omar / lina   password: reading-room-2026
-- The admin account is created by AdminBootstrap from ADMIN_* in .env.
-- =====================================================================

BEGIN;

DO $seed$
BEGIN
    IF EXISTS (SELECT 1 FROM books) THEN
        RAISE NOTICE 'Seed skipped: the books table already has data.';
        RETURN;
    END IF;

    -- -----------------------------------------------------------------
    -- Authors
    -- -----------------------------------------------------------------
    INSERT INTO authors (name, birth_date, bio) VALUES
        ('Fyodor Dostoevsky',          '1821-11-11', 'Russian novelist whose books dig into guilt, faith and freedom.'),
        ('Mary Shelley',               '1797-08-30', 'English novelist who wrote Frankenstein while still a teenager.'),
        ('Ursula K. Le Guin',          '1929-10-21', 'American writer of science fiction and fantasy, known for her anthropological imagination.'),
        ('Carl Sagan',                 '1934-11-09', 'Astronomer and science communicator who made the cosmos feel close.'),
        ('Toni Morrison',              '1931-02-18', 'Nobel laureate whose novels explore Black life and memory in America.'),
        ('Italo Calvino',              '1923-10-15', 'Italian writer of playful, inventive and structurally daring fiction.'),
        ('Marcus Aurelius',            NULL,         'Roman emperor and Stoic philosopher.'),
        ('Gabriel García Márquez',     '1927-03-06', 'Colombian novelist and a central figure of magical realism.'),
        ('Yuval Noah Harari',          '1976-02-24', 'Israeli historian writing about the long arc of human history.'),
        ('Chimamanda Ngozi Adichie',   '1977-09-15', 'Nigerian writer of novels, stories and essays.'),
        ('Haruki Murakami',            '1949-01-12', 'Japanese novelist of dreamlike, melancholy fiction.'),
        ('George Orwell',              '1903-06-25', 'English essayist and novelist, sharp critic of totalitarianism.'),
        ('Jane Austen',                '1775-12-16', 'English novelist of manners, money and marriage.'),
        ('Charlotte Brontë',           '1816-04-21', 'English novelist and the eldest of the Brontë sisters to survive childhood.'),
        ('Chinua Achebe',              '1930-11-16', 'Nigerian novelist and critic, a founding voice of modern African literature.'),
        ('Stephen Hawking',            '1942-01-08', 'Theoretical physicist who wrote about black holes and the origins of the universe.'),
        ('Richard Dawkins',            '1941-03-26', 'Evolutionary biologist and popular science author.'),
        ('Jared Diamond',              '1937-09-10', 'Geographer and historian of human societies.'),
        ('Plato',                      NULL,         'Athenian philosopher and student of Socrates.'),
        ('Friedrich Nietzsche',        '1844-10-15', 'German philosopher and cultural critic.'),
        ('Thomas S. Kuhn',             '1922-07-18', 'Historian and philosopher of science.'),
        ('Frank Herbert',              '1920-10-08', 'American science fiction author.');

    -- -----------------------------------------------------------------
    -- Books: one staging row per title, then fanned out into the real tables
    -- -----------------------------------------------------------------
    CREATE TEMP TABLE seed_books (
        isbn        VARCHAR(13),
        title       VARCHAR(500),
        published   DATE,
        description TEXT,
        authors     TEXT[],
        genres      TEXT[],
        copies      INT
    ) ON COMMIT DROP;

    INSERT INTO seed_books VALUES
        ('9780140449136', 'Crime and Punishment', '1866-01-01',
         'A poor former student in St. Petersburg commits a murder and spends the rest of the novel unravelling under the weight of it.',
         ARRAY['Fyodor Dostoevsky'], ARRAY['Fiction', 'Classics'], 4),
        ('9780374528379', 'The Brothers Karamazov', '1880-01-01',
         'Three very different brothers and a murdered father, in Dostoevsky''s last and largest argument about God and responsibility.',
         ARRAY['Fyodor Dostoevsky'], ARRAY['Fiction', 'Classics', 'Philosophy'], 2),
        ('9780141439471', 'Frankenstein', '1818-01-01',
         'A young scientist builds a living creature and then abandons it, with consequences for both of them.',
         ARRAY['Mary Shelley'], ARRAY['Fiction', 'Classics', 'Science Fiction'], 3),
        ('9780441478125', 'The Left Hand of Darkness', '1969-03-01',
         'An envoy visits a frozen planet whose people have no fixed sex, and has to rethink nearly everything he assumes.',
         ARRAY['Ursula K. Le Guin'], ARRAY['Science Fiction'], 2),
        ('9780061054884', 'The Dispossessed', '1974-05-01',
         'A physicist leaves his anarchist moon for the wealthy planet next door, carrying a theory both worlds want.',
         ARRAY['Ursula K. Le Guin'], ARRAY['Science Fiction'], 2),
        ('9780345539434', 'Cosmos', '1980-01-01',
         'A tour of the universe and of the people who worked out how it fits together.',
         ARRAY['Carl Sagan'], ARRAY['Science'], 3),
        ('9780345376596', 'Pale Blue Dot', '1994-01-01',
         'Sagan looks back at Earth from the edge of the solar system and asks what our future in space should be.',
         ARRAY['Carl Sagan'], ARRAY['Science'], 2),
        ('9781400033416', 'Beloved', '1987-09-02',
         'After the Civil War, a woman who escaped slavery is visited by a presence from the past she cannot put down.',
         ARRAY['Toni Morrison'], ARRAY['Fiction', 'Classics'], 2),
        ('9781400033423', 'Song of Solomon', '1977-01-01',
         'Milkman Dead goes looking for gold and finds his family''s history instead.',
         ARRAY['Toni Morrison'], ARRAY['Fiction'], 2),
        ('9780156453806', 'Invisible Cities', '1972-01-01',
         'Marco Polo describes impossible cities to Kublai Khan, each one a short meditation on memory and desire.',
         ARRAY['Italo Calvino'], ARRAY['Fiction'], 3),
        ('9780156439619', 'If on a winter''s night a traveler', '1979-01-01',
         'A novel about you, the reader, trying and failing to finish a novel.',
         ARRAY['Italo Calvino'], ARRAY['Fiction'], 2),
        ('9780140449334', 'Meditations', '2006-04-27',
         'Private notes a Roman emperor wrote to himself about duty, death and staying calm.',
         ARRAY['Marcus Aurelius'], ARRAY['Philosophy', 'Classics'], 4),
        ('9780060883287', 'One Hundred Years of Solitude', '1967-05-30',
         'Seven generations of the Buendía family in the town of Macondo, where the miraculous is ordinary.',
         ARRAY['Gabriel García Márquez'], ARRAY['Fiction', 'Classics'], 3),
        ('9780307389732', 'Love in the Time of Cholera', '1985-01-01',
         'A man waits more than fifty years to tell the woman he loves that he still loves her.',
         ARRAY['Gabriel García Márquez'], ARRAY['Fiction'], 2),
        ('9780062316097', 'Sapiens', '2011-01-01',
         'A brisk history of how one species of ape came to run the planet.',
         ARRAY['Yuval Noah Harari'], ARRAY['History'], 4),
        ('9780307455925', 'Americanah', '2013-05-14',
         'Two teenagers in love leave Nigeria for America and Britain, and find each other again years later.',
         ARRAY['Chimamanda Ngozi Adichie'], ARRAY['Fiction'], 2),
        ('9781400095209', 'Half of a Yellow Sun', '2006-08-11',
         'Lives in a university town are upended by the Nigerian civil war.',
         ARRAY['Chimamanda Ngozi Adichie'], ARRAY['Fiction', 'History'], 2),
        ('9781400079278', 'Kafka on the Shore', '2002-09-12',
         'A runaway teenager and an old man who talks to cats follow paths that slowly converge.',
         ARRAY['Haruki Murakami'], ARRAY['Fiction'], 2),
        ('9780375704024', 'Norwegian Wood', '1987-09-04',
         'A student in 1960s Tokyo remembers a love divided between two very different women.',
         ARRAY['Haruki Murakami'], ARRAY['Fiction'], 2),
        ('9780679775430', 'The Wind-Up Bird Chronicle', '1994-04-12',
         'A man searching for his missing cat, and then his missing wife, ends up at the bottom of a well.',
         ARRAY['Haruki Murakami'], ARRAY['Fiction'], 1),
        ('9780451524935', 'Nineteen Eighty-Four', '1949-06-08',
         'Winston Smith rewrites history for the Party and quietly begins to rebel.',
         ARRAY['George Orwell'], ARRAY['Fiction', 'Classics'], 4),
        ('9780451526342', 'Animal Farm', '1945-08-17',
         'The animals take over the farm, and the pigs take over the animals.',
         ARRAY['George Orwell'], ARRAY['Fiction', 'Classics'], 3),
        ('9780141439518', 'Pride and Prejudice', '1813-01-28',
         'Elizabeth Bennet and Mr Darcy misjudge each other, at length and with great wit.',
         ARRAY['Jane Austen'], ARRAY['Fiction', 'Classics'], 3),
        ('9780141441146', 'Jane Eyre', '1847-10-16',
         'An orphaned governess insists on her own dignity in a house with a secret.',
         ARRAY['Charlotte Brontë'], ARRAY['Fiction', 'Classics'], 2),
        ('9780385474542', 'Things Fall Apart', '1958-01-01',
         'Okonkwo, a proud Igbo leader, watches his world change as missionaries and colonial rule arrive.',
         ARRAY['Chinua Achebe'], ARRAY['Fiction', 'Classics'], 3),
        ('9780553380163', 'A Brief History of Time', '1988-04-01',
         'Black holes, the Big Bang and the nature of time, explained for readers without the maths.',
         ARRAY['Stephen Hawking'], ARRAY['Science'], 3),
        ('9780198788607', 'The Selfish Gene', '1976-01-01',
         'Evolution told from the point of view of the gene rather than the organism.',
         ARRAY['Richard Dawkins'], ARRAY['Science'], 2),
        ('9780393354324', 'Guns, Germs, and Steel', '1997-03-01',
         'Why some societies came to dominate others, argued from geography, crops and disease.',
         ARRAY['Jared Diamond'], ARRAY['History', 'Science'], 2),
        ('9780140455113', 'The Republic', '2007-04-26',
         'Socrates and friends try to define justice by imagining the ideal city.',
         ARRAY['Plato'], ARRAY['Philosophy', 'Classics'], 2),
        ('9780140441185', 'Thus Spoke Zarathustra', '1883-01-01',
         'A prophet comes down from the mountain to announce the death of God and the coming of the overman.',
         ARRAY['Friedrich Nietzsche'], ARRAY['Philosophy'], 2),
        ('9780226458120', 'The Structure of Scientific Revolutions', '1962-01-01',
         'How science really changes: not steadily, but through crises and paradigm shifts.',
         ARRAY['Thomas S. Kuhn'], ARRAY['Science', 'Philosophy'], 1),
        ('9780441172719', 'Dune', '1965-08-01',
         'A noble family takes over a desert planet that holds the most valuable substance in the universe.',
         ARRAY['Frank Herbert'], ARRAY['Science Fiction', 'Classics'], 4);

    INSERT INTO books (title, isbn, published_date, description)
    SELECT title, isbn, published, description FROM seed_books;

    INSERT INTO book_authors (book_id, author_id)
    SELECT b.id, a.id
    FROM seed_books s
    JOIN books b ON b.isbn = s.isbn
    CROSS JOIN LATERAL unnest(s.authors) AS author_name
    JOIN authors a ON a.name = author_name;

    INSERT INTO book_genres (book_id, genre)
    SELECT b.id, genre
    FROM seed_books s
    JOIN books b ON b.isbn = s.isbn
    CROSS JOIN LATERAL unnest(s.genres) AS genre;

    -- Barcodes: LMS-<last 6 ISBN digits>-<copy number>
    INSERT INTO book_copies (book_id, barcode)
    SELECT b.id, 'LMS-' || right(s.isbn, 6) || '-' || lpad(n::text, 2, '0')
    FROM seed_books s
    JOIN books b ON b.isbn = s.isbn
    CROSS JOIN LATERAL generate_series(1, s.copies) AS n;

    -- One copy went missing
    UPDATE book_copies SET status = 'LOST' WHERE barcode = 'LMS-439471-03';

    -- -----------------------------------------------------------------
    -- Members, and reader accounts for three of them
    -- -----------------------------------------------------------------
    INSERT INTO members (full_name, email, phone, created_at, updated_at) VALUES
        ('Amara Okafor',    'amara.okafor@example.com',    '+20 100 482 1937', now() - interval '210 days', now() - interval '210 days'),
        ('Omar Farouk',     'omar.farouk@example.com',     '+20 122 305 7741', now() - interval '160 days', now() - interval '160 days'),
        ('Lina Haddad',     'lina.haddad@example.com',     NULL,               now() - interval '120 days', now() - interval '120 days'),
        ('Tomás Herrera',   'tomas.herrera@example.com',   '+34 611 204 583',  now() - interval '95 days',  now() - interval '95 days'),
        ('Kenji Watanabe',  'kenji.watanabe@example.com',  NULL,               now() - interval '80 days',  now() - interval '80 days'),
        ('Sofia Lindqvist', 'sofia.lindqvist@example.com', '+46 70 318 2265',  now() - interval '61 days',  now() - interval '61 days'),
        ('Priya Raman',     'priya.raman@example.com',     NULL,               now() - interval '40 days',  now() - interval '40 days'),
        ('Daniel Mensah',   'daniel.mensah@example.com',   '+233 24 719 0352', now() - interval '12 days',  now() - interval '12 days');

    INSERT INTO users (username, email, password_hash, member_id, created_at, updated_at)
    SELECT v.username, m.email,
           -- BCrypt of the development password documented at the top of this file
           '$2a$10$BO35DE1ByzNQAULTfjYasua0fpJEs5VVggKAKmdhOAiOMEwkN2RaC',
           m.id, m.created_at, m.created_at
    FROM (VALUES ('amara', 'amara.okafor@example.com'),
                 ('omar',  'omar.farouk@example.com'),
                 ('lina',  'lina.haddad@example.com')) AS v(username, email)
    JOIN members m ON m.email = v.email
    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE lower(u.username) = v.username OR lower(u.email) = v.email);

    INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id
    FROM users u
    JOIN roles r ON r.name = 'ROLE_USER'
    WHERE u.username IN ('amara', 'omar', 'lina')
    ON CONFLICT DO NOTHING;

    -- -----------------------------------------------------------------
    -- Loans. Days are counted back from now(); loan period is 14 days and
    -- late returns are charged 0.50 per day, matching application.properties.
    -- Open loans keep fine_amount 0: the API calculates the fine on return.
    -- -----------------------------------------------------------------
    INSERT INTO loans (copy_id, member_id, borrowed_at, due_date, returned_at, fine_amount)
    SELECT c.id,
           m.id,
           now() - v.borrowed_days * interval '1 day',
           now() - v.borrowed_days * interval '1 day' + interval '14 days',
           CASE WHEN v.returned_days IS NULL THEN NULL ELSE now() - v.returned_days * interval '1 day' END,
           CASE WHEN v.returned_days IS NOT NULL AND v.borrowed_days - v.returned_days > 14
                THEN 0.50 * (v.borrowed_days - v.returned_days - 14)
                ELSE 0 END
    FROM (VALUES
            -- member email,                 barcode,         borrowed, returned (days ago)
            ('amara.okafor@example.com',    'LMS-478125-01',  4,  NULL),
            ('amara.okafor@example.com',    'LMS-079278-01', 17,  NULL),   -- overdue
            ('amara.okafor@example.com',    'LMS-449136-01', 40,  30),
            ('amara.okafor@example.com',    'LMS-449334-02', 60,  44),     -- returned 2 days late
            ('omar.farouk@example.com',     'LMS-172719-01',  9,  NULL),
            ('omar.farouk@example.com',     'LMS-316097-01', 20,  NULL),   -- overdue
            ('omar.farouk@example.com',     'LMS-474542-01', 30,  20),
            ('lina.haddad@example.com',     'LMS-524935-01',  2,  NULL),
            ('lina.haddad@example.com',     'LMS-455925-01', 25,  15),
            ('tomas.herrera@example.com',   'LMS-033416-01', 19,  NULL),   -- overdue
            ('tomas.herrera@example.com',   'LMS-539434-02', 50,  30),     -- returned 6 days late
            ('kenji.watanabe@example.com',  'LMS-704024-01', 11,  NULL),
            ('kenji.watanabe@example.com',  'LMS-528379-01', 30,  NULL),   -- overdue
            ('sofia.lindqvist@example.com', 'LMS-439518-01',  6,  NULL),
            ('priya.raman@example.com',     'LMS-380163-01', 12,  5)
         ) AS v(email, barcode, borrowed_days, returned_days)
    JOIN members m ON m.email = v.email
    JOIN book_copies c ON c.barcode = v.barcode;

    UPDATE book_copies
    SET status = 'ON_LOAN'
    WHERE id IN (SELECT copy_id FROM loans WHERE returned_at IS NULL);

    RAISE NOTICE 'Seeded % authors, % books, % copies, % members, % loans.',
        (SELECT count(*) FROM authors), (SELECT count(*) FROM books),
        (SELECT count(*) FROM book_copies), (SELECT count(*) FROM members),
        (SELECT count(*) FROM loans);
END
$seed$;

COMMIT;
