import { join, resolve } from "https://deno.land/std@0.107.0/path/mod.ts";
import { copy } from "https://deno.land/std@0.107.0/fs/mod.ts";

const input = resolve("src");
const output = resolve("output");
const outputSrc = join(output, "src"); // compatibility between meta and app-meta branch

onDirectory(".");
copy("public", output, { overwrite: true });

async function onDirectory(dir: string) {
  for await (const entry of Deno.readDir(join(input, dir))) {
    if (entry.isFile) await onFile(dir, entry);
    else await onDirectory(join(dir, entry.name));
  }
}

async function onFile(dir: string, entry: Deno.DirEntry) {
  const name = entry.name;
  const path = join(input, dir, name);
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
