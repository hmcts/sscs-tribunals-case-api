import winston from "winston";


const customFormat = winston.format.printf(( { level, message }) => {
    return `[${level}] : ${message}`; 
});

const options = {
    level: "info",
    format: customFormat,
    transports: [
        new winston.transports.Console({
            level: "info"
        }),
        /* Commenting this code to check with Platops if we can push log files */
        // new winston.transports.File({
        //     level: "info",
        //     filename: './logs/test-automation.log'
        // })
    ]
}

const logger = winston.createLogger(options);

export default logger;