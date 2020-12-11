import {useRouter} from "next/router";
import React, {useContext, useEffect, useState} from "react";

import Form from "react-bootstrap/Form";

import {getTrip, TRIP_BASE_PATH} from "../../lib/trip";
import {UserContext} from "../../contexts/UserContext";
import Button from "react-bootstrap/Button";
import {BOOKING_BASE_PATH, bookTrip} from "../../lib/booking";
import axios from "axios";

export default function BookingForm() {

    const router = useRouter()
    const {tripId} = router.query

    const {state} = useContext(UserContext)

    const [trip, setTrip] = useState(null)
    const [amount, setAmount] = useState("1")
    const [error, setError] = useState(false)

    useEffect(() => {
        if (tripId) {
            return axios
                .get(TRIP_BASE_PATH + "/trips/" + tripId)
                .then(res => {
                    res.data.data
                    setTrip(res.data.data)
                })
                .catch(err => {
                    console.error(err)
                    return false
                })
        }
    }, [tripId])

    const updateTrip = async (tripId) => {
        const trip = await getTrip(tripId)
        setTrip(trip)
    }

    const handleSubmit = async (event) => {
        event.preventDefault()
        const location = await bookTrip(state.user.username, tripId, amount)
        if (!location) setError(true)
        else {
            setError(false)
            const bookingId = location.split(BOOKING_BASE_PATH + "/bookings/").pop()
            await router.push("/bookings/" + bookingId)
        }
    }

    if (!state.user) return (
        <h1>Must be signed in to book a trip</h1>
    )
    if (!trip) return null

    return (
        <Form
            onSubmit={handleSubmit}
            className={"border border-dark rounded mx-5 mx-auto p-5 mw-80"}
        >
            {error && <p className={"text-danger"}>REQUEST FAILED</p>}
            <h1>{trip.title}</h1>
            <p>{trip.description}</p>
            <p>Before: <strike>{trip.price + 200}</strike>, now only <strong>{trip.price}!</strong></p>
            <p><strong>Location:</strong> {trip.location}</p>
            <p><strong>Start:</strong> {new Date(trip.start).toLocaleString()}</p>
            <p>
                <strong>Duration:</strong> {trip.duration.days} days, {trip.duration.hours} hours, {trip.duration.minutes} minutes
            </p>
            <Form.Group controlId="amount">
                <Form.Label>Amount:</Form.Label>
                <Form.Control as="select" onChange={e => setAmount(e.target.value)}>
                    <option>1</option>
                    <option>2</option>
                    <option>3</option>
                    <option>4</option>
                    <option>5</option>
                </Form.Control>
            </Form.Group>
            <h5>Total price: {trip.price * amount}</h5>
            <Button type={"submit"}>Book trip!</Button>

        </Form>
    )

};