import {useRouter} from "next/router";
import React, {useContext, useEffect, useState} from "react";
import axios from "axios";

import Form from "react-bootstrap/Form";

import {TRIP_BASE_PATH} from "../../lib/trip";
import {UserContext} from "../../contexts/UserContext";
import Button from "react-bootstrap/Button";
import {BOOKING_BASE_PATH, cancelBooking, updateAmount} from "../../lib/booking";
import Card from "react-bootstrap/Card";

export default function BookingForm() {

    const router = useRouter()
    const {id} = router.query

    const {state} = useContext(UserContext)

    const [booking, setBooking] = useState(null)
    const [trip, setTrip] = useState(null)
    const [amount, setAmount] = useState("1")
    const [bump, setBump] = useState(0)
    const [error, setError] = useState(false)

    useEffect(() => {
        if (id) {
            console.log("Updating booking")
            axios
                .get(BOOKING_BASE_PATH + "/bookings/" + id)
                .then(res => {
                    setBooking(res.data.data)
                })
                .catch(err => {
                    console.error(err)
                    return false
                })
        }
    }, [id, bump])

    useEffect(() => {
        if (booking) {
            return axios
                .get(TRIP_BASE_PATH + "/trips/" + booking.tripId)
                .then(res => {
                    res.data.data
                    setTrip(res.data.data)
                    setAmount(booking.amount)
                })
                .catch(err => {
                    console.error(err)
                    return false
                })
        }
    }, [booking])


    const handleCancel = async (event) => {
        event.preventDefault()
        await cancelBooking(booking.id)
        setBump(bump + 1)
    }

    const handleUpdateAmount = async (event) => {
        event.preventDefault()
        const res = await updateAmount(booking.id, amount)
        if (!res) setError(true)
        else {
            setError(false)
            setBump(bump + 1)
        }
    }

    function isSelected(id) {
        if (amount == id){
            return "selected"
        }
    }

    if (!state.user) return (
        <h1>Must be signed in to book a trip</h1>
    )
    if (!booking) return null
    if (!trip) return (
        <Card
            style={{width: '30rem'}}
            className={"m-2"}
            border={"dark"}>
            <Card.Body>
                <Card.Subtitle>Trip has been cancelled.</Card.Subtitle>
                <Card.Text>Booking ID: {booking.id}</Card.Text>
                <Card.Text>Trip ID: {booking.tripId}</Card.Text>
                <Card.Text>Amount: {booking.amount}</Card.Text>
            </Card.Body>
        </Card>
    )
    return (
        <Form
            onSubmit={e => e.preventDefault()}
            className={"border border-dark rounded mx-5 mx-auto p-5 mw-80"}
        >
            {error && <p className={"text-danger"}>REQUEST FAILED</p>}
            {booking.cancelled && <h1 className={"text-danger"}>CANCELLED</h1>}
            <h1>Booking:</h1>
            <h3>{trip.title}</h3>
            <p>{trip.description}</p>
            <p><strong>Amount:</strong> {booking.amount}</p>
            <p><strong>Start:</strong> {new Date(trip.start).toLocaleString()}</p>
            {!booking.cancelled && <>
                <Form.Group controlId="amount">
                    <Form.Label>Change amount:</Form.Label>
                    <Form.Control as="select" onChange={e => setAmount(e.target.value)}>
                        <option>1</option>
                        <option>2</option>
                        <option>3</option>
                        <option>4</option>
                        <option>5</option>
                    </Form.Control>
                </Form.Group>
                <Button className={"m-1"} onClick={handleCancel} type={"submit"}>Cancel booking!</Button>
                {amount != booking.amount &&
                <Button className={"m-1"} onClick={handleUpdateAmount} type={"submit"}>Update amount!</Button>}
            </>}

        </Form>
    )

};