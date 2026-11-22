module.exports = {
  schemaPath: process.env.OPENAPI_SCHEMA_URL || 'http://localhost:8080/v3/api-docs',
  serversPath: './src/services',
  requestImportStatement: "import request from 'umi-request';",
};
