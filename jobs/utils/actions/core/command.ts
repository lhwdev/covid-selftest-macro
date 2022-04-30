// deno-lint-ignore-file no-explicit-any
import { toCommandValue } from "./utils.ts";

// For internal use, subject to change.

// We use any as a valid input type

export interface CommandProperties {
  [key: string]: any;
}

/**
 * Commands
 *
 * Command Format:
 *   ::name key=value,key=value::message
 *
 * Examples:
 *   ::warning::This is the message
 *   ::set-env name=MY_VAR::some value
 */
export function issueCommand(
  command: string,
  properties: CommandProperties,
  message: any,
): void {
  const cmd = new Command(command, properties, message);
  console.log(cmd.toString());
}

export function issue(name: string, message = ""): void {
  issueCommand(name, {}, message);
}

const CMD_STRING = "::";

class Command {
  private readonly command: string;
  private readonly message: string;
  private readonly properties: CommandProperties;

  constructor(command: string, properties: CommandProperties, message: string) {
    if (!command) {
      command = "missing.command";
    }

    this.command = command;
    this.properties = properties;
    this.message = message;
  }

  toString(): string {
    let cmdStr = CMD_STRING + this.command;

    if (this.properties && Object.keys(this.properties).length > 0) {
      cmdStr += " ";
      let first = true;
      for (const key in this.properties) {
        if (Object.hasOwn(this.properties, key)) {
          const val = this.properties[key];
          if (val) {
            if (first) {
              first = false;
            } else {
              cmdStr += ",";
            }

            cmdStr += `${key}=${escapeProperty(val)}`;
          }
        }
      }
    }

    cmdStr += `${CMD_STRING}${escapeData(this.message)}`;
    return cmdStr;
  }
}

function escapeData(s: any): string {
  return toCommandValue(s)
    .replace(/%/g, "%25")
    .replace(/\r/g, "%0D")
    .replace(/\n/g, "%0A");
}

function escapeProperty(s: any): string {
  return toCommandValue(s)
    .replace(/%/g, "%25")
    .replace(/\r/g, "%0D")
    .replace(/\n/g, "%0A")
    .replace(/:/g, "%3A")
    .replace(/,/g, "%2C");
}
