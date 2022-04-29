import { exec } from "./execute.ts";
import { parse } from "https://deno.land/std@0.128.0/path/mod.ts";
export default async function sparseClone(
  { targetPath, url, sparsePathCone, ref }: {
    targetPath: string;
    url: string;
    sparsePathCone?: string;
    ref: string;
  },
) {
  const parsed = parse(targetPath);
  const path = exec.cd(targetPath);
  await exec.cd(parsed.dir).execute([
    "git",
    "clone",
    "--depth=1",
    "--no-checkout",
    "--single-branch",
    "--branch",
    ref,
    url,
    parsed.base,
  ]);

  if (sparsePathCone) {
    await path.execute(["git", "sparse-checkout", "set", sparsePathCone]);
  }
  await path.execute(["git", "checkout", ref]);
}
