/**
 * Minifies and publishes the content of meta branch into app-meta.
 * 
 * Requirements:
 * - Directory structure:
 *   * Input: ${args[0]}/src/.., ${args[0]}/public/..
 *   * Output: ${args[1]}/output/..
 * - Arguments: as specified in above, [input directory, output directory]
 */
import publishMain from "./main";

const [inputBase, outputBase] = Deno.args;
await publishMain(inputBase, outputBase);
