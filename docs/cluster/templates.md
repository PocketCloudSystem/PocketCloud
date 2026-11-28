# Templates & replication

Every node keeps a **full copy** of the template folders. The head owns the valid version of each folder and
nodes only fetch files they do not have. You can change template files on **any** node (SFTP, editor, a plugin
upload); the change reaches all other nodes within a few seconds.

## What is replicated

Each replicated folder is called a *unit*:

| Unit | Folder | Name in commands |
|------|--------|------------------|
| Template | `templates/<name>/` | `Lobby` |
| Global template | `templates/global/<type>/` | `global/server`, `global/proxy` |
| Group | `groups/<name>/` | `groups/<name>` or `group:<name>` |
| Definitions (only with `provider: json`) | `templates/templates.json`, `groups/groups.json` | `meta` |

Not replicated:

- the folders of **static servers** (they live on their template's `staticNode` only),
- server software and the bridge jar (every node downloads them itself),
- files matched by the ignore rules below.

Templates are copied into a server folder only when the server is **prepared**. Running servers never notice a sync.

## How a change travels

1. Every node watches `templates/` and `groups/` (Java `WatchService`, every sub folder registered on its own).
2. After **3 s without file events** the node lists the changed folder (size and modification time of every file)
   and checks again a second later. If something still moves (an SFTP upload in progress), it waits again.
3. The node sends a **delta** to the head: the version it last took over (the base), the new or changed files and
   the removed files (with debug logging: `Sent 1 changed and 0 removed file(s) of Lobby to head node-1.`).

4. The head merges the delta into its current version, fetches the new files from the node (256 KiB chunks over
   the cluster connection), stores the new version and announces it:

   ```text
   Template Lobby was updated (changed on node-2).
   ```

   The version number and the number of changed files appear with debug logging and in `template history`.

   Changes made on the head itself are taken over directly.
5. Every other node compares the announced version with its copy, fetches the file list (manifest: path, size,
   SHA-256) and **only the files it does not have yet**, builds the new version in a temporary folder and swaps it
   in while holding the template's write lock. A server that is being prepared at the same moment gets either the
   old or the new version, never a mix.

   ```text
   Template Lobby was updated (changed on node-2).
   ```

   The node where the change was made logs nothing more; the version, the number of fetched files and their size
   appear with debug logging. A change of `templates.json`/`groups.json` is logged as
   `The template and group settings were updated (changed on node-2).`

In addition every node compares all units with the head every 30 s and right after (re)connecting to it.

A node never lets a head overwrite its copy with an **older** version than the one it took over last (this can
happen after a head change, see below). It keeps its copy and sends it to the head, which makes it a new version:

```text
This node has a newer copy of template Lobby than the head; keeping it and sending it to the cluster.
```

Deleting a template folder deletes it on all nodes (head: `Template Lobby was deleted (on node-2).`, other nodes:
`Template Lobby was deleted on another node and removed here.`).

With the `json` provider, creating, editing or removing a template or group on one node changes
`templates.json`/`groups.json`, which are replicated like files. The other nodes reload them:

```text
Template Lobby was created on another node and is now available here.
Settings of template Lobby were updated from the cluster.
Template Lobby was removed on another node.
```

## S3 storage

By default the head sends the files to every node itself. With `templateStorage: s3` in `cluster.yml` (see
[Configuration](configuration.md#cluster)) the files go through an S3 bucket instead (AWS S3, MinIO,
Cloudflare R2 or any other S3-compatible service):

- Before the head announces a new version, it uploads its new files to the bucket, each file once under its
  SHA-256 (`<prefix>blobs/<sha256>`). The other nodes download them from the bucket, so the head uploads a
  change once instead of sending it to every node.
- Versions, file lists, conflicts, pause and rollback still go through the cluster connection. Changes made on a
  follower still reach the head over the cluster connection.
- When a node becomes head it also uploads the current versions that are not in the bucket yet (in the background).
- If the bucket cannot be reached or a file is missing there, the nodes get the file from the head as before. The
  head then logs `Could not upload template Lobby to S3, the nodes get it from this node instead: ...`.

Credentials go into `storage/internal/cluster/s3.properties` on every node (not into `cluster.yml`, which is often
shared), or into the environment variables `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`:

```properties
accessKey=AKIA...
secretKey=...
```

The key needs `s3:GetObject` and `s3:PutObject` on the prefix; with `s3:ListBucket` a missing file is reported as
missing instead of "access denied" (both fall back to the head). On startup each node logs
`Template files are synced through S3 (https://.../pocketcloud/templates/).` The bucket is never cleaned up:
files of old versions stay there; delete the prefix by hand to clean it up.
Files larger than 5 GB cannot be uploaded (one upload per file).

## Restarting servers after a change

A change reaches servers that start afterwards; running servers keep their copy. To bring them up to date without
fewer servers or kicked players, run `template restart Lobby` (see [Commands](commands.md)) or list the template in
`restartOnUpdate` in `cluster.yml` to do it automatically after every change:

```text
Restarting 3 server(s) of Lobby one after another...
Restarted Lobby-1.
Restarted Lobby-2.
Restarted Lobby-3.
All servers of Lobby were restarted.
```

If a step takes longer than 3 minutes (e.g. no node can start a server: `No node can start a server of Lobby right
now: ...`), the restart stops with `Rolling restart of Lobby stopped: ...`; the servers not restarted yet keep running.

## Starting a server with an outdated copy

When the head starts a server on a node, it sends the versions it expects for the template, its global template and
its groups. If the node's copy is different, the node syncs first (`Template Lobby was updated.`) and starts
afterwards. The expected and the local version appear with debug logging
(`Template Lobby is at version 6 here, the head expects 7: syncing it before starting Lobby-4.`).

The start fails (and the head retries on another node, see [Scheduling](scheduling.md#start-retries)) if

- the sync does not finish within 7 s: `template version not synced within 7 s`,
- the node has local changes the head does not have yet:
  `the template Lobby has local changes on node-2 that are not replicated yet`, or
- the sync of the template is paused on the node and its last taken-over version is not the one the head expects:
  `the sync of Lobby is paused on node-2 and it does not have version 7` (see [Pausing the sync](#pausing-the-sync)).

## Head changes

A new head does not trust its own disk copy. Before it serves any template it asks every reachable node which
version of every unit it has (the version it took over last, or the version it served while it was head itself),
and takes the **highest version** of each unit (same version with different content: the answer of the previous
head wins, then its own). This happens silently; with debug logging:

```text
Template replication: collected the template versions of 3 node(s) (node-2, node-1, node-3).
Template Lobby was at version 2 here, the cluster has version 3 (from node-3): took it over before serving.
Template replication: serving 4 unit(s) as head (0 new version(s), 1 taken over from other nodes).
```

- If the new head is behind, it fetches that version (from a node whose folder or history has it) before serving.
- If its folder is **missing**, the template is restored the same way. A missing folder only becomes a deletion when
  the newest version in the cluster is a deletion.
- Local changes on the new head that no head had seen yet are merged onto the newest version and become the next
  version (`Template Lobby was outdated here and had local changes; merged them into the cluster version.`).
- If no reachable node can serve the newest version, the head serves its own copy as a new version
  (`The newest version of template Lobby is on no reachable node; using the copy of this node.`), or, if it has no
  copy, does not serve the template until a node that has it offers it again
  (`Template Lobby is missing here and no reachable node has it. ...`). It never deletes it.
- A node that was not reachable while the head collected the versions and has a newer one sends it when it
  reconnects (see above); the head also copies the history entries it missed from that node.


## Ignored files

Never replicated (defaults):

```text
*.tmp  *.temp  *.part  *.swp  *.swx  ~*  *~  *.filepart  .filepart
.DS_Store  Thumbs.db  *.log  *.log.gz  *.lck
logs/  cache/  .cache/  cloud_log_archive/
```

A template can add its own rules in a `.pcignore` file in its folder (one glob per line, `#` for comments):

```text
# no slash: matches the name of a file or folder anywhere
*.bak
# trailing slash: matches folders with that name anywhere
backups/
# slash inside: matches the path relative to the template folder
worlds/tmp/**
```

Ignored files stay local on each node. The `.pcignore` file itself is replicated.

## Conflicts

The delta contains the base version. If the same file was changed on two nodes since that base, the **later change
(the one that reaches the head last) wins**. The other version stays in the history and the head warns:

```text
Conflict in Lobby/plugins/MyPlugin/config.yml: changed on node-3 and node-2 at the same time. The change from node-3 is kept, the other one stays in the history.
```

Changes to different files on different nodes are simply merged. To avoid conflicts, make larger changes on one
node only, or pause the sync while you work (see below).

## History and rollback

The head keeps the **last 10 versions** of every unit (every file content is stored once) in
`storage/internal/cluster/template-history/`. Server saves (`saveOnShutdown`, `server save`) that write into a
template show up as their own entry (`save of Lobby-2`).

```text
template history Lobby
```

```text
History of Lobby (current version: 8, head: node-1):
v8 | 2026-11-22 14:03:11 | node node-2 | changed on node-2 | 1 changed, 0 removed | ~plugins/MyPlugin/config.yml
v7 | 2026-11-22 13:58:40 | node node-3 | changed on node-3 (conflict) | 1 changed, 0 removed | conflicts: plugins/MyPlugin/config.yml | ~plugins/MyPlugin/config.yml
...
```

```text
template rollback Lobby 6
```

restores version 6 as a **new** version (`Rolled back template Lobby to version 6.`) and distributes it. Both commands work on every node; on a follower they are forwarded to the head.
`history` and `rollback` take the unit names from the table above (e.g. `template history global/server`).

The history survives a head change. It is replicated lazily:

- after a follower takes over a version, it copies the history entries it does not have yet from the head
  (metadata and file list of each version, at most what fits into one 1 MB packet) and keeps the files of that
  version as content-addressed blobs (`template-history/<unit>/blobs/<sha256>`). So every node has the history
  entries and the files of every version it applied itself;
- a new head copies missing entries from the node it takes the newest version from, and from a node that pushes a
  version newer than the head's;
- a rollback to a version whose files the head does not have fetches them from any node that still has them
  (with debug logging: `Fetched plugins/conf.yml of Lobby for the rollback from node-1.`).

A version made while a node was offline is missing in that node's history (it never saw it); `template history`
on a new head shows the entries it has. Like on the head, every follower keeps the files of the last 10 versions,
so the history needs about as much disk space as the templates themselves (on each node).

## Pausing the sync

For larger rebuilds:

```text
template sync pause Lobby     # one template
template sync pause           # all templates
template sync resume Lobby
template sync resume
template sync status
```

Pausing applies to **the node where you run the command** and lasts until `resume`, also across restarts of that
node (stored in `storage/internal/cluster/template-sync/paused.json`; on start the node warns
`Template sync is still paused on this node for: Lobby.`). While a unit is paused on a node:

- local changes there are neither sent to the head nor overwritten by the head's version;
- servers never start from the half-finished copy. Servers that start **on that node** get the **last distributed
  version** (the version the node took over last, kept as history blobs), not the local changes:

  ```text
  Sync of Lobby is paused here: starting Lobby-1 with the last synced version, not the local changes.
  Sync of Lobby is paused here: used the last synced version, not the local changes.
  ```

- if a newer version was distributed meanwhile, the node does not have it: it refuses starts of that template
  (`the sync of Lobby is paused on node-2 and it does not have version 5`) and reports this in its heartbeat, so the
  head starts the servers on other nodes (`template sync paused there` in the node selection);
- if the sync is paused **on the head**, changes from other nodes to that unit wait until it is resumed there;
- other nodes keep the last distributed version and start their servers with it.

So run `pause` on the node where you edit. `resume` scans the folder and sends everything that changed at once.

`template sync status` lists every unit with version, short hash, file count and state (`in sync`,
`outdated (head vN)`, `changed`, `sending`, `paused`, `deleted`).

## Where templates are edited from the console

`template create`, `template edit`, `template remove`, `template nodes` and `template staticnode` work on every
node. With the `json` provider they change `templates.json` on that node, which is then replicated like any other
file (concurrent edits of the definitions on two nodes are a conflict on that file, the later one wins).
HTTP API writes to templates and groups are always forwarded to the head (see [HTTP API](http-api.md)).
