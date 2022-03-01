
import { join, resolve } from "https://deno.land/std@0.107.0/path/mod.ts";
import { copy } from "https://deno.land/std@0.107.0/fs/mod.ts";


export default async function publishMain(inputBase: string, outputBase: string) {
  const src = join(inputBase, "src");
  const public = join(inputBase, "public");

  const output = join(outputBase, "output");
  const outputSrc = join(output, "src"); // compatibility between meta and app-meta branch

  // 1. Minify
  await onDirectory(src);
  await copy(public, output, { overwrite: true });

  async function onDirectory(dir: string) {
    for await (const entry of Deno.readDir(dir)) {
      if (entry.isFile) await onFile(dir, entry);
      else await onDirectory(join(dir, entry.name));
    }
  }

  async function onFile(dir: string, entry: Deno.DirEntry) {
    const name = entry.name;
    const path = join(dir, name);
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
        try {
          Deno.mkdir(toDir, { recursive: true });
        } catch (_) {
          // when directory exists already, this throws error
        }
        await Deno.writeTextFile(join(toDir, name), result);
        break;
      }
    }
  }


  // 2. Publish
}
