import React from "react";
import { Photo, Pill } from "./Ui";
import { inr, kg } from "../format";

export default function ProductCard({ item, onOpen }) {
  return (
    <button type="button" className="card" onClick={() => onOpen(item)} aria-label={`${item.cropName}, ${inr(item.pricePerKg)} per kg`}>
      <div className="card__photo">
        <Photo src={item.imageUrl} alt={item.cropName} />
        {item.location && <span className="card__tag">{item.location}</span>}
      </div>
      <div className="card__body">
        <h3 className="card__title">{item.cropName}</h3>
        <span className="card__shop">Sold by {item.sellerName}</span>
        <div className="card__row">
          <span className="price">{inr(item.pricePerKg)}<small> / kg</small></span>
          {item.quantityKg > 0 ? <small className="muted">{kg(item.quantityKg)} left</small> : <Pill value="SOLD_OUT" />}
        </div>
      </div>
    </button>
  );
}
