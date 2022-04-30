// Used the output of https://github.com/JamesIves/github-pages-deploy-action
import { join } from "https://deno.land/std@0.128.0/path/mod.ts";
import { copy } from "https://deno.land/std@0.128.0/fs/mod.ts";
import { exec } from "../utils/execute.ts";
import { tempDirectory } from "../utils/actions-temp-directory.ts";

export default async function publish(
  { sourcePath, targetRepository, ref, gitUserName, gitUserEmail }: {
    sourcePath: string;
    targetRepository: string;
    ref: string;
    gitUserName: string;
    gitUserEmail: string;
  },
) {
  const targetDir = exec.cd(join(tempDirectory, "publish", crypto.randomUUID()));
  await targetDir.execute(["git", "config", "user.name", gitUserName]);
  await targetDir.execute(["git", "config", "user.email", gitUserEmail]);

  // if (Deno.env.get("CI")) {
  //   await targetDir.execute(["git", "config", "--local", "--unset-all", `http.${context.serverUrl}/.extraheader`]);
  // }

  await targetDir.execute(
    ["git", "clone", "--no-recurse-submodules", "--depth=1", "--single-branch", "-b", ref, targetRepository],
  );

  await targetDir.execute(["git", "worktree", "add", "--no-checkout", "--detach", targetDir.cwd]);
  await targetDir.execute(["git", "checkout", "-B", ref, `origin/${ref}`]);

  await copy(sourcePath, targetDir.cwd);

  await targetDir.execute(["git", "add", "--all", "."]);
}
