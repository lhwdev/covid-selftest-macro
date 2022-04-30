// For internal use, subject to change.

// We use any as a valid input type
// deno-lint-ignore-file no-explicit-any
import { eol } from "./core.ts";

import { toCommandValue } from "./utils.ts";

export function issueCommand(command: string, message: any): void {
  const filePath = Deno.env.get(`GITHUB_${command}`);
  if (!filePath) {
    throw new Error(
      `Unable to find environment variable for file command ${command}`,
    );
  }

  Deno.writeTextFileSync(filePath, `${toCommandValue(message)}${eol}`, { append: true });
}
