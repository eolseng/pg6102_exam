import axios from 'axios'

export const TRIP_BASE_PATH = "/api/v1/trip"

export async function getTrip(tripId) {
    return axios
        .get(TRIP_BASE_PATH + "/trips/" + tripId)
        .then(res => res.data.data)
        .catch(err => {
            console.error(err)
            return false
        })
}