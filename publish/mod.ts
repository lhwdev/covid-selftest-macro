import { resolve } from "https://deno.land/std@0.107.0/path/mod.ts";

const input = "src";
const output = "public";

onDirectory(".");

async function onDirectory(dir: string) {
  for await (const entry of Deno.readDir(resolve(input, dir))) {
    if (entry.isFile) onFile(dir, entry);
    else onDirectory(`${dir}/${entry.name}`);
  }
}

async function onFile(dir: string, entry: Deno.DirEntry) {
  const name = entry.name;
  const path = resolve(input, `${dir}/${entry.name}`);
  const index = name.lastIndexOf(".");
  const extension = index == -1 ? null : name.slice(index + 1);
  switch (extension) {
    case null: {
      break;
    }

    case "json": {
      const string = await Deno.readTextFile(path);
      const result = JSON.stringify(JSON.parse(string));
      await Deno.writeTextFile(resolve(output, `${dir}/${name}`), result);
      break;
    }
  }
}
