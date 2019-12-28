# Stack Overflow Recommender Outline

The basic concept here is to read in a question by URL or ID, and
recommend existing duplicates.

An easier first step is just to allow the user to record particular
questions as personal canonicals for some set of keywords.

https://api.stackexchange.com has the SE API, which is a
straightforward JSON/REST interface.

The basic outline needs to look like so:

    Crawler --> Fetcher --> ML pipeline / indexer --> Querier
                   |                  ^
                   v                  |
                 Text of posts in local files

The API docs really strongly encourage good hygiene around API calls,
including rate limiting (max 30 calls/second and even that's
considered excessive) and batching.  If we decide to poll (for
instance for new questions) it needs to be rate-limited to 1/minute.

Objects have lists of fields; you can register a custom filter (once,
at design time, hard-coding its URL thereafter) to request specific
combinations of fields.

The API interface is reasonably documented.  There is not, for
example, a Swagger specification for the API format.  There are a
couple of SDKs built around the API but the only one that seems to be
supported is the Javascript one.  The API seems to be very stable (no
changes to it in 5 years) so it's probably reasonable to structure our
internal representation around it.
