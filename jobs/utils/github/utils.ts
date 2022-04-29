import * as Context from "./context.ts";
import * as Utils from "./internal/utils.ts";

// octokit + plugins
import { Octokit } from "https://cdn.skypack.dev/@octokit/core?dts";
import { restEndpointMethods } from "https://cdn.skypack.dev/@octokit/plugin-rest-endpoint-methods?dts";
import { paginateRest } from "https://cdn.skypack.dev/@octokit/plugin-paginate-rest?dts";

export const context = new Context.Context();

const baseUrl = Utils.getApiBaseUrl();
const defaults = {
  baseUrl,
  // request: {
  //   agent: Utils.getProxyAgent(baseUrl),
  // },
};

export const GitHub = Octokit.plugin(
  restEndpointMethods,
  paginateRest,
).defaults(defaults);

/**
 * Convience function to correctly format Octokit Options to pass into the constructor.
 *
 * @param     token    the repo PAT or GITHUB_TOKEN
 * @param     options  other options to set
 */
export function getOctokitOptions(
  token: string,
  // deno-lint-ignore no-explicit-any
  options?: any, // @types {import("@octokit/core/dist-types/types.d.ts").OctokitOptions}
  // deno-lint-ignore no-explicit-any
): any {
  const opts = Object.assign({}, options || {}); // Shallow clone - don't mutate the object provided by the caller

  // Auth
  const auth = Utils.getAuthString(token, opts);
  if (auth) {
    opts.auth = auth;
  }

  return opts;
}
