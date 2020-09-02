const {resolve} = require("path");
// const Copy = require('copy-webpack-plugin');
const static = resolve(__dirname, "static");

module.exports = {
    mode: 'production',
    entry: {
        bundle: "./src/index.tsx"
    },
    devtool: "source-map",
    output: {
        filename: "bundle.js",
        path: static,
        publicPath: "/"
    },
    resolve: {
        extensions: ['.js', '.ts', '.tsx']
    },
    node: {
        console: true,
        fs: 'empty',
        net: 'empty',
        tls: 'empty'
    },
    // plugins: [
    //     new Copy([
    //         { from: resolve(__dirname, "src/assets") , to: static }
    //     ])
    // ],
    module: {
        rules: [
            {
                test: [/\.tsx?$/],
                use: [
                    {loader: 'ts-loader', options: {transpileOnly: true}}
                ]
            },
            {
                test: /\.scss|css$/,
                use: [
                    {
                        loader: "style-loader"
                    },
                    {
                        loader: "css-loader"
                    },
                    {
                        loader: "sass-loader"
                    }
                ]
            },
            {
                test: /\.(jpg|png|pdf)$/,
                use: [
                    {
                        loader: 'file-loader'
                    }
                ]
            },
            {
                test: /\.(png|jpg|gif)$/i,
                use: [
                    {
                        loader: 'url-loader',
                        options: {
                            limit: 8192
                        }
                    }
                ]
            }
        ]
    }
};