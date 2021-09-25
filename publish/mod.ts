import { resolve } from "https://deno.land/std@0.107.0/path/mod.ts";

const input = "src";
const output = "output";

Deno.mkdirSync(resolve(output), { recursive: true })

onDirectory(".");

async function onDirectory(dir: string) {
  for await (const entry of Deno.readDir(resolve(input, dir))) {
    if (entry.isFile) await onFile(dir, entry);
    else await onDirectory(`${dir}/${entry.name}`);
  }
}

async function onFile(dir: string, entry: Deno.DirEntry) {
  const name = entry.name;
  const path = resolve(input, `${dir}/${name}`);
  const index = name.lastIndexOf(".");
  const extension = index == -1 ? null : name.slice(index + 1);
  switch (extension) {
    case null: {
      break;
    }

    case "json": {
      const string = await Deno.readTextFile(path);
      const result = JSON.stringify(JSON.parse(string));
      const toDir = resolve(output, dir);
      try {
        Deno.mkdir(toDir, { recursive: true });
      } catch(_) {
        // when directory exists already, this throws error
      }
      await Deno.writeTextFile(`${toDir}/${name}`, result);
      break;
    }
  }
}
