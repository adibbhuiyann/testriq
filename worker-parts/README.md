# worker.js in four parts

The Cloudflare code editor on a phone often cuts off a long paste. These four files are `worker.js`
split at function boundaries (about 10 KB each). Paste them into the editor one after another:

1. Delete everything in the editor, then paste `part-1.txt`.
2. Without moving the cursor, paste `part-2.txt`, then `part-3.txt`, then `part-4.txt`.
3. The editor should end with `the whole worker is in. Tap Deploy.` Tap **Deploy**.

The marker lines (`// ===== …`) are comments and are safe to leave in.
If `worker.js` changes, regenerate these files from it.
