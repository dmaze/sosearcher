# Reference management

(Internal design notes.)

Some objects refer to others with a straightforward one-to-many
relationship.  Let's pick on answers referring to questions.  There
are three forms this can take:

1. In the Stack Exchange API, an
   [answer](https://api.stackexchange.com/docs/types/answer) has an
   integer `question_id` field

2. In the database schema, the `answer` table has a `question_id
   INTEGER FOREIGN KEY REFERENCES questions(id)`, where the database
   chooses the question ID (it is different from the API ID)

3. In code, the `Answer` object has a `question: Question` object
   reference

The code will split these into attributes and relations.  There will
be separate type hierarchies for these three things, sharing a set of
attributes that are constant across the three layouts.

This would seem like a straightforward case for a type parameter:

```scala
case class Answer[Ref] {
  question: Ref[Question]
}
```

Matching the three reference types, each of the forms can have
additional data.  In particular the API will return at least some of
the data inline, and not necessarily in formats that match the
database layout.  (This is fine and not a deficiency in the API.)  A
question contains both an inline array of answers (if requested) and
the ID of an accepted answer (if any).  Users are a more obvious case
where a relatively small amount of user data is always included
inline.

If these are going to be really different (a `APIRef[User]` is just an
outright different object from a `APIRef[Site]`; a `APIRef[Answer]` in
a question could be two different things depending on whether it's the
accepted answer or the inline answer array) then the
attribute/relations split makes more sense.

