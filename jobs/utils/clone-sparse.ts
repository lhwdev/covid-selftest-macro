import { exec } from "./execute.ts";

export default async function sparseClone(
  { targetPath, url, sparsePathCone, ref }: {
    targetPath: string;
    url: string;
    sparsePathCone?: string;
    ref: string;
  },
) {
  const path = exec.cd(targetPath);
  await exec.execute([
    "git",
    "clone",
    "--depth=1",
    "--no-checkout",
    "--single-branch",
    "--branch",
    ref,
    url,
    path.cwd,
  ]);

  if (sparsePathCone) {
    await path.execute(["git", "sparse-checkout", "set", sparsePathCone]);
  }
  await path.execute(["git", "checkout", ref]);
}
