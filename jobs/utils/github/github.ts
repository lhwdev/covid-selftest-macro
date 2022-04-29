import * as Context from "./context.ts";
import { getOctokitOptions, GitHub } from "./utils.ts";

// octokit + plugins
import { OctokitOptions } from "https://cdn.skypack.dev/@octokit/core/dist-types/types?dts";

export const context = new Context.Context();

/**
 * Returns a hydrated octokit ready to use for GitHub Actions
 *
 * @param     token    the repo PAT or GITHUB_TOKEN
 * @param     options  other options to set
 */
export function getOctokit(
  token: string,
  options?: OctokitOptions,
): InstanceType<typeof GitHub> {
  return new GitHub(getOctokitOptions(token, options));
}
