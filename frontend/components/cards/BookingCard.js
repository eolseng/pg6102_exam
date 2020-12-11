import React, {useContext, useEffect, useState} from "react";
import Card from "react-bootstrap/Card";
import {UserContext} from "../../contexts/UserContext";
import Link from "next/link";
import axios from "axios";
import {TRIP_BASE_PATH} from "../../lib/trip";

export default function BookingCard({booking}) {

    const {state} = useContext(UserContext)

    const [trip, setTrip] = useState(null)

    useEffect(() => {
        if (booking) {
            return axios
                .get(TRIP_BASE_PATH + "/trips/" + booking.tripId)
                .then(res => {
                    res.data.data
                    setTrip(res.data.data)
                })
                .catch(err => {
                    console.error(err)
                    return false
                })
        }
    }, [booking])


    if (!booking) return null
    if (!state.user) return (
        <h1>Must be signed in to view bookings</h1>
    )
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
        <Card
            style={{width: '30rem'}}
            className={"m-2"}
            border={"dark"}
        >
            <Card.Body>
                <Card.Subtitle className={"mb-2 text-muted"}>
                    <span>Id: {booking.id} - Amount: {booking.amount}</span>
                </Card.Subtitle>
                <Card.Title>{trip.title}</Card.Title>
                <Card.Text>{trip.description}</Card.Text>
                <Link href={"/bookings/" + booking.id}><a>View booking</a></Link>
            </Card.Body>
            {booking.cancelled &&
            <Card.Footer className={"text-center text-muted"}>
                <h3>BOOKING CANCELLED</h3>
            </Card.Footer>
            }
        </Card>
    )

}