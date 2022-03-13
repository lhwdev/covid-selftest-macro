import { join } from "https://deno.land/std@0.128.0/path/mod.ts";
import { ensureDirSync } from "https://deno.land/std@0.128.0/fs/mod.ts";

// See https://github.com/actions/toolkit/blob/4964b0cc7c5aaa2557b79cfe4bdb42dd657f6df0/packages/cache/src/internal/cacheUtils.ts#L13
function createTempDirectory(): string {
  const IS_WINDOWS = Deno.build.os === "windows";

  let tempDirectory = Deno.env.get("RUNNER_TEMP");

  if (!tempDirectory) {
    let baseLocation: string;
    if (IS_WINDOWS) {
      // On Windows use the USERPROFILE env variable
      baseLocation = Deno.env.get("USERPROFILE") || "C:\\";
    } else {
      if (Deno.build.os === "darwin") {
        baseLocation = "/Users";
      } else {
        baseLocation = "/home";
      }
    }
    tempDirectory = join(baseLocation, "actions", "temp");
  }

  const dest = join(tempDirectory, crypto.randomUUID());
  ensureDirSync(dest);
  return dest;
}

export const tempDirectory: string = createTempDirectory()
