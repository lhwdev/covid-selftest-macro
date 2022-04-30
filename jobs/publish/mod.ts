/**
 * Minifies and publishes the content of meta branch into app-meta.
 *
 * Requirements:
 * - Repository:
 *   * Cloned to meta branch (secure) so that this js file exists
 * - Directory structure:
 *   * Input: ${args[0]}/src/.., ${args[0]}/public/..
 *   * Output: ${args[1]}/output/..
 * - Arguments: as specified in above, [input directory, output directory]
 */
import publishMain from "./main.ts";

const [inputBase, outputBase, token] = Deno.args;
await publishMain(inputBase, outputBase, token);
