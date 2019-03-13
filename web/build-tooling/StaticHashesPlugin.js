/* eslint-disable class-methods-use-this */
/* eslint-disable no-restricted-syntax */
import { createHash } from 'crypto';
import path from 'path';
import slash from 'slash';

/**
 * Webpack plugin to hash all of the assets and write that hash to a single file.
 */
module.exports = class StaticHashesPlugin {
  constructor({ dest = 'WEB-INF/static-hashes.properties', base = false, hash = 'md5', delimiter = '=', maxLength = 12, salt = 'a' } = {}) {
    this.options = {
      dest,
      base,
      hash,
      delimiter,
      maxLength,
      salt,
    };
  }

  apply(compiler) {
    compiler.hooks.emit.tapAsync('StaticHashesPlugin', (compilation, done) => {
      const { assets } = compilation;
      const { dest, hash, delimiter, maxLength, salt } = this.options;
      const hashes = {};

      const base = this.options.base || path.dirname(dest);

      for (const filename in assets) {
        if (Object.prototype.hasOwnProperty.call(assets, filename)) {
          const filePath = path.relative(base, filename);

          let fileHash =
            createHash(hash)
              .update(assets[filename].source(), 'binary')
              .update(salt)
              .digest('hex');

          fileHash = parseInt(fileHash, 16);
          if (fileHash < 0) fileHash = -fileHash;

          fileHash = fileHash.toPrecision(21).replace(/[^\d]/g, '');
          if (maxLength && fileHash.length > maxLength) {
            fileHash = fileHash.substring(0, maxLength);
          }

          hashes[slash(filePath)] = fileHash;
        }
      }

      const lines = Object.keys(hashes).sort().map((key) => `${key}${delimiter}${hashes[key]}\n`);
      const contents = lines.join('');

      const data = new Buffer(contents);

      assets[dest] = {
        source: () => data,
        size: () => data.length,
      };

      done();
    });
  }
};
