import React, {useCallback, useEffect, useRef, useState} from "react";
import axios from "axios";
import Spinner from "react-bootstrap/Spinner";
import {Button} from "react-bootstrap";
import {BOOKING_BASE_PATH} from "../../lib/booking";
import BookingCard from "../cards/BookingCard";

export default function BookingContainer() {

    // Uses infinite scrolling, but only works once first page is filled.

    const pageSize = 6;

    const [bookings, setBookings] = useState([])
    const [next, setNext] = useState(BOOKING_BASE_PATH + "/bookings?amount?=" + pageSize)
    const [loading, setLoading] = useState(false)
    const [page, setPage] = useState(0)
    const loader = useRef(null)

    useEffect(() => {
        const options = {
            root: null,
            rootMargin: '0px',
            threshold: 0.25
        }
        const observer = new IntersectionObserver(intersectHandler, options)
        if (loader.current && next) {
            observer.observe(loader.current)
        }
    }, []);

    // Handler on intersect
    const intersectHandler = useCallback((entries) => {
        const target = entries[0]
        if (target.isIntersecting && next) {
            setPage((n) => n + 1)
        }
    }, [])

    useEffect(() => {
        loadMore()
    }, [page])

    // Fetch the next page
    const fetchNextPage = async () => await axios.get(next).then(res => res.data.data).catch(() => false)

    // Fetch and add data into array
    const loadMore = () => {
        if (next) {
            setLoading(true)
            fetchNextPage()
                .then(data => {
                    // Update 'next' page path
                    if (data.next) setNext(data.next)
                    else setNext(null)
                    // Add new Blueprints to list
                    setBookings([...bookings, ...data.list])
                }).catch(() => false)
            setLoading(false)
        }
    }

    return (
        <div>
            <h1>Your bookings:</h1>
            <div className={"d-flex flex-wrap"}>
                {bookings.map(booking => <BookingCard key={booking.id} booking={booking}/>)}
            </div>
            {loading &&
            <Spinner animation={"border"} role={"status"}>
                <span className={"sr-only"}>Loading...</span>
            </Spinner>}
            {next &&
            <Button ref={loader} onClick={loadMore}>Load more</Button>
            }
        </div>
    )

};