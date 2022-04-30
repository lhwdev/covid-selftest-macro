import { context } from "../utils/github/github.ts";
import { join } from "https://deno.land/std@0.128.0/path/mod.ts";
import { copy, ensureDir } from "https://deno.land/std@0.128.0/fs/mod.ts";
import sparseClone from "../utils/clone-sparse.ts";
import { exec } from "../utils/execute.ts";
import config from "./config.ts";

export default async function publishMain(input: string, temp: string) {
  await ensureDir(temp);

  const src = join(input, "src");
  const publicDir = join(input, "public");

  const output = join(temp, "output");
  const outputSrc = join(output, "src"); // compatibility between meta and app-meta branch

  const dirRoot = src;
  async function onDirectory(dir: string) {
    for await (const entry of Deno.readDir(join(dirRoot, dir))) {
      if (entry.isFile) await onFile(dir, entry);
      else await onDirectory(join(dir, entry.name));
    }
  }

  async function onFile(dir: string, entry: Deno.DirEntry) {
    const name = entry.name;
    const path = join(dirRoot, dir, name);
    const index = name.lastIndexOf(".");
    const extension = index == -1 ? null : name.slice(index + 1);
    switch (extension) {
      case null: {
        break;
      }

      case "json": {
        const string = await Deno.readTextFile(path);
        const result = JSON.stringify(JSON.parse(string));
        const toDir = join(outputSrc, dir);
        await ensureDir(toDir);
        await Deno.writeTextFile(join(toDir, name), result);
        break;
      }
    }
  }

  /// 1. Minify
  await ensureDir(output);
  await onDirectory(".");
  await copy(publicDir, output, { overwrite: true });

  /// 2. Publish
  const repo = exec.cd(join(temp, "repo"));

  const userName = context.payload.pusher.name;

  const urlBody = context.serverUrl.slice(context.serverUrl.indexOf("://") + 3);
  await sparseClone({
    targetPath: repo.cwd,
    url: `https://${userName}:${context.token!}@${urlBody}/${context.repo.owner}/${context.repo.repo}`,
    ref: config.targetRef,
  });

  await repo.execute(["git", "config", "user.name", userName]);
  await repo.execute(["git", "config", "user.email", context.payload.pusher.email]);

  await copy(output, repo.cwd, { overwrite: true });

  const previous = context.payload.head_commit;
  const commitMessage = previous ? `🚀 Deploy@${previous.id}: ${previous.message}` : "🚀 Deploy from app-meta";

  const needsCommit = !!(await repo.executeAsync(["git", "status", "--porcelain"]).resultText());
  if (needsCommit) {
    await repo.execute(["git", "add", "-A"]);
    await repo.execute(["git", "commit", "-m", commitMessage]);

    await repo.execute(["git", "push"]);
  } else {
    console.log("skip commit as nothing has changed");
    await repo.execute(["git", "status"]);
  }
}
