# Type Examples
- **String:** `hello`
- **Integer:** `2023`
- **Long:** `242385234992037888`
- **Array:** `element1,element2,element3`
- **Map:** `{key1:"value1",key2:"value2",key3:"value3"}`
# Database/static
**API Prefix:** https://eventalerts.gg/api/v1/ENDPOINT
## Comparisons
- `equals`: The document's value must be the same as the given one
- `contains`: The document's value must contain all of the given ones
## Misc
- `unique`: Only 1 document can have this value, so only 1 will be returned, always (unless something goes terribly wrong)
## Endpoints
- `servers`
  - **Example:** https://eventalerts.gg/api/v1/servers?representatives=242385234992037888,365630764244664320&name=test
    Will return servers with the name `test` that have srnyx *and* Oiiink as representatives
  - **Query Parameters**
    - `id`, string, equals, unique
    - `message`, long, equals, unique
    - `name`, string, equals
    - `description`, string, equals
    - `invite`, string, equals
    - `created`, long, equals
    - `tags`, string array, contains
    - `color`, integer, equals
    - `thumbnail`, string, equals
    - `gets`, string array, contains key
    - `representatives`, long array, contains
- `players`
  - **Example:** https://eventalerts.gg/api/v1/players?boosterPasses=314853603695394817
    Will return any user that has given Skeppy a Booster Pass (should just be one, as you can only get a Booster Pass from one person at a time)
  - **Query Parameters**
    - `id`, string, equals, unique
    - `user`, long, equals, unique
    - `anniversaries`, integer array, contains
    - `boosterPasses`, long array, contains
- `events`
  - **Example:** https://eventalerts.gg/api/v1/events?channel=980956946075115570
    Will get all active events in the Partner events channel
  - **Query Parameters**
    - `id`, string, equals, unique
    - `thread`, long, equals, unique
    - `channel`, long, equals
    - `title`, string, equals
    - `time`, long, equals
    - `subscribers`, long array, contains
# Websockets
**API Prefix:** `wss://eventalerts.venox.network/api/v1/socket/SOCKET`
*You must use `wss://`, not `ws://`, as our sockets are encrypted!*
## Conditions
- `builder`: Only included if the event was posted using `/event builder`
## Endpoints
- `server_enabled`
  - **Keys**
    - `id`, string
    - `representatives`, long array
    - `created`, long
    - `name`, string
    - `description`, string
    - `invite`, string
    - `tags`, string array
    - `color`, integer
    - `thumbnail`, string
    - `message`, long
    - `gets`, [string, string] map
- `server_edited`
  - **Keys**
    - `id`, string
    - `representatives`, long array
    - `created`, long
    - `name`, string
    - `description`, string
    - `invite`, string
    - `tags`, string array
    - `color`, integer
    - `thumbnail`, string
    - `message`, long
    - `gets`, [string, string] map
- `booster_pass_given`
  - **Keys**
    - `id`, string
    - `representatives`, long array
    - `created`, long
    - `name`, string
    - `description`, string
    - `invite`, string
    - `tags`, string array
    - `color`, integer
    - `thumbnail`, string
    - `message`, long
    - `gets`, [string, string] map
- `event_posted`
  - **Keys**
    - `id`, string
    - `user`, long
    - `anniversaries`, integer array
    - `booster_passes`, long array
- `event_cancelled`
  - **Keys**
    - `channel`, long
    - `thread`, long
    - `id`, string
    - `title`, string
    - `time`, long, builder
    - `subscribers`, long array, builder
- `potential_famous_event`
  - This will just be a string of the message that contained the role ping
- `famous_event`
  - This will just be a string of the message that contained the role ping