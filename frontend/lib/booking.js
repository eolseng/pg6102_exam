import axios from 'axios'

export const BOOKING_BASE_PATH = "/api/v1/booking"

/**
 * Books a trip, returns the location header
 */
export async function bookTrip(username, tripId, amount) {
    return axios
        .post(BOOKING_BASE_PATH + "/bookings", {
            username: username,
            tripId: tripId,
            amount: amount
        })
        .then(res => res.headers.location)
        .catch(() => false)
}

export async function cancelBooking(bookingId) {
    return axios
        .patch(BOOKING_BASE_PATH + /bookings/ + bookingId, {command: "CANCEL"})
        .then(res => res.status === 204)
        .catch(() => false)
}

export async function updateAmount(bookingId, amount) {
    return axios
        .patch(BOOKING_BASE_PATH + /bookings/ + bookingId, {command: "UPDATE_AMOUNT", newAmount: amount})
        .then(res => res.status === 204)
        .catch(() => false)
}