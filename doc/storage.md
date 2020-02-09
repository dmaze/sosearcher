# Storage

The disconnect between the Stack Exchange APIs and a reasonable
relational database model is pretty frustrating.  Especially as I'm
starting to wire in questions, which have a little bit of internal
structure and a lot of attributes, I'm having to type the list of
attributes, a dozen long, in four or five places.

Why can't we just store the JSON response from the API?  A simple
file-system storage could work for this, or [there are databases that
do that](https://en.wikipedia.org/wiki/Document-oriented_database).
The trick, as always, is following links between objects.

Let's focus on
[questions](https://api.stackexchange.com/docs/types/question).  In
addition to its static attributes, a question has relations to sites,
answers, other questions (duplicates), comments, and users, plus a
question has tags that are searchable and its Markdown body.  These
are generally referred to by ID, though some or all of the content may
be included in the question itself (if requested) and the site is
purely contextual.

## Core Modelling

Looking at the capabilities of the databases listed below, the most
obvious thing that jumps out is that relationships are generally most
efficient when represented as direct links to other records.  If every
record has a UUID, then question (UUID1) belongs to site (UUID2) and
was written by user (UUID3).  Depending on the database you ofter can
do an indirect lookup (the field `owner.user_id` in a question matches
the field `user_id` in a user) but this tends to not be the
recommended style.

If we want to model this as API responses first, then there should be
a document "kind" that represents a fetch.  That can include the
actual URL, relevant timestamps, an HTTP status code, and (if it was
successful) the JSON response body.  We'd prefer to not mangle this
further.

After we've fetched something, we then need to connect it to other
documents.  Even if they're generally one-to-many, storing a separate
"kind" of relationships works here.

Note that this model gives us room for placeholder objects that don't
exist yet.  Say we determine that question 1 is a duplicate of
question 2.  We can create a fetch record with no body for question 2
and immediately create the relationship, even if we haven't started to
fetch things yet.

Correspondingly, we'll need a step to scan an object for its outbound
relationships, look up or create the corresponding fetch record, kick
off the fetch if necessary, and return the (potentially new) record
ID.  This becomes an actually interesting use of the actor system.

## Quick Paper Review

Open-source alternatives from the Wikipedia page linked above that
aren't obviously unsuitable:

[ArangoDB](https://www.arangodb.com/) has a mixed document/graph
model.  It's kind of a kitchen-sink database, and it also includes a
text-analysis module.  There is [a pattern for one-to-many
joins](https://www.arangodb.com/docs/stable/aql/examples-join.html#one-to-many)
but it requires keeping the actual document ID in the source document;
a more typical pattern seems to involve a separate collection of edges
(from/to/attributes), and possibly using its graph-traversal module.

[CouchDB](https://couchdb.apache.org) focus on replication; its query
interface is Javascript map/reduce code.  You _can_ do [various sorts
of joins](https://docs.couchdb.org/en/stable/ddocs/views/joins.html),
but mostly by getting really friendly with the key-index interface and
writing involved Javascript view code.

[Elasticsearch](https://www.elastic.co/elasticsearch) is mostly about
gluing a JSON document store on to Solr for text indexing.
Historically it hasn't done joins at all, except for parent/child type
relationships, and the Elasticsearch 7.5 document still [describes
joins](https://www.elastic.co/guide/en/elasticsearch/reference/current/joining-queries.html)
as "prohibitively expensive".

[OrientDB](https://orientdb.org) has an object-oriented style: objects
have classes and there is a class hierarchy, and there seems to be a
heavy Java focus.  Classes have (partial) schemas and there is an
explicit `LINK` field type; you can embed objects in other objects
(embed comments in questions) but cannot directly access the embedded
object.  You can also build a graph structure with explicit edge objects.

[PostgreSQL](https://www.postgresql.org) is one of the standard
relational databases, but it has a [JSON column
type](https://www.postgresql.org/docs/12/datatype-json.html) and
supports SQL/JSON path queries (this is apparently a technical term).
It's unclear how well JSON content can be indexed, but you could do
joins using ordinary SQL join syntax.

[RethinkDB](https://rethinkdb.com) emphasizes a push model: instead of
polling the database for updated content, the database pushes updates
back to the application.  Its core data model is JSON with field-type
extensions.  There's support for [joins by
key](https://rethinkdb.com/docs/data-modeling/) and an extended
[general-purpose join
mechanism](https://rethinkdb.com/docs/table-joins/).
