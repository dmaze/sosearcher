# Document fetching flow

(Internal design notes.)

1. The user enters a questions URL.
2. The controller parses this out into its site and question ID; if
   this fails, return immediately.
3. The controller sends a request to fetch the question to an actor.
4. The metadata-fetching actor batches up question IDs.  When it has
   100 or 1 second has passed, request the metadata from the SE API
   (everything interesting except the body).  Successful question IDs
   are passed to a body-fetching actor.
5. The body-fetching actor batches up question IDs.  When it has 100
   or 1 second has passed, request the question bodies only from the
   SE API.
6. Notify the controller, which can send an HTTP response.

Batch sizes and timeouts are arbitrary.  There can be two different
values for these for user-initiated fetches and for crawls.

The database layout needs to support a couple of states.  Roughly this is:

**Site**: API parameter, URL, ...

**Fetch Request**: Site ID, post type (`QUESTION`), object number,
fetch type (`METADATA`, `BODY`), timestamp, result

**Question**: Site ID, question number, closed date, closed reason,
creation date, link, locked date, owner ID, protected date, score,
title

**Tag**: Question ID, tag

**User**: Site ID, SE user ID, type, display name, link, profile
image, reputation

Since SQLite doesn't really support enums, keep fetch and post types
in support tables.  (We expect these to never change except in
migrations.)

Question bodies are stored in the local filesystem.  (Given the actual
size of SO questions, the database would be fine, but I want to model
the small metadata/large body architecture here.)

As with sites, there should be separate actors dedicated to database
and API operations plus a separate controller.  The state-machine
model is straightforward if there is an actor per site.  There's
almost no cost to keeping an unused actor running (especially given at
most dozens of sites) but they will need to be created dynamically.

For the moment only the specific question fields will be fetched.
Partly this is to simplify the amount of database schema required.  In
general I anticipate a need to re-fetch questions and to support
schema migrations: at some point the code will need to be able to
fetch data that wasn't fetched the first time, so go ahead and allow
that for answers.  (The alternative is doing an exhaustive fetch of
all data in the SE API and hoping the API never changes; again that
might be appropriate for this specific project but isn't the right
general practice.)
