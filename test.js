
const r = require('request-promise-native');

const BLOCKED= 'http://localhost:8080/blocked'
const UNBLOCKED= 'http://localhost:8080/unblocked'
const URL = UNBLOCKED

const startTime = new Date();

const promises = [...Array(100)].map(() => r.get(URL).promise())

Promise.all(promises).then((results) => {
	const endTime = new Date();
	console.log(Math.round((endTime - startTime)/1000));
	results.forEach((result) => console.log(result))
});