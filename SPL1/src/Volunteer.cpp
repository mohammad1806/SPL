#include "../include/Volunteer.h"
#include <iostream>

Volunteer ::Volunteer(int id, const string &name) : completedOrderId(NO_ORDER), activeOrderId(NO_ORDER), id(id), name(name) {}
int Volunteer ::getId() const
{
  return id;
}
const string &Volunteer::getName() const
{
  return name;
}

int Volunteer::getActiveOrderId() const
{
  return activeOrderId;
}
int Volunteer::getCompletedOrderId() const
{
  return completedOrderId;
}

bool Volunteer::isBusy() const
{
  return activeOrderId != -1;
}

//<--------------------------------------------------------------------------------------------->

CollectorVolunteer::CollectorVolunteer(int id, const string &name, int coolDown) : Volunteer(id, name), coolDown(coolDown), timeLeft() {}

int CollectorVolunteer::getCoolDown() const
{
  return coolDown;
}

int CollectorVolunteer::getTimeLeft() const
{
  return timeLeft;
}

bool CollectorVolunteer ::decreaseCoolDown()
{
  timeLeft--;

  if (this->timeLeft <= 0)
  {
    return true;
  }

  return false;
}

void CollectorVolunteer::step()
{
  if (decreaseCoolDown())
  {
    this->completedOrderId = this->activeOrderId;
    activeOrderId = NO_ORDER;
  }
}

bool CollectorVolunteer ::hasOrdersLeft() const
{
  return true;
}

bool CollectorVolunteer::canTakeOrder(const Order &order) const
{
  if (!isBusy() && order.getStatus() == OrderStatus::PENDING)
  {
    return true;
  }
  else
  {
    return false;
  }
}

void CollectorVolunteer::acceptOrder(const Order &order)
{
  if (canTakeOrder(order))
  {
    activeOrderId = order.getId();
    this->timeLeft = this->coolDown;
  }
}
string CollectorVolunteer::toString() const
{
  string result = "VolunteerID: " + std::to_string(getId());
  if (isBusy())
  {
    result += "\nisBusy: true \n";
  }
  else
  {
    result += "\nisBusy: false \n";
  }

  if (isBusy())
  {
    result += "OrderID: " + std::to_string(getActiveOrderId()) + "\n";
  }
  else
  {
    result += "OrderID: None \n";
  }

  if (activeOrderId == NO_ORDER)
  {
    result += "timeLeft: None \n";
  }
  else
  {
    result += "timeLeft : " + std::to_string(getTimeLeft()) + "\n";
  }
  result += "OrdersLeft: NoLimit";

  return result;
}

CollectorVolunteer *CollectorVolunteer::clone() const
{
  return new CollectorVolunteer(*this);
}

//<--------------------------------------------------------------------------------------------->

LimitedCollectorVolunteer::LimitedCollectorVolunteer(int id, const string &name, int coolDown, int maxOrders)
    : CollectorVolunteer(id, name, coolDown), maxOrders(maxOrders), ordersLeft(maxOrders) {}

int LimitedCollectorVolunteer::getMaxOrders() const
{
  return maxOrders;
}

int LimitedCollectorVolunteer::getNumOrdersLeft() const
{
  return ordersLeft;
}
bool LimitedCollectorVolunteer::hasOrdersLeft() const
{
  return ordersLeft != 0;
}
bool LimitedCollectorVolunteer::canTakeOrder(const Order &order) const
{
  if (hasOrdersLeft() && CollectorVolunteer::canTakeOrder(order))
  {
    return true;
  }
  return false;
}

void LimitedCollectorVolunteer::acceptOrder(const Order &order)
{
  if (canTakeOrder(order))
  {
    CollectorVolunteer::acceptOrder(order);
    ordersLeft--;
  }
}

string LimitedCollectorVolunteer ::toString() const
{
  string result = "VolunteerID: " + std::to_string(getId());
  if (isBusy())
  {
    result += "\nisBusy: true \n";
  }
  else
  {
    result += "\nisBusy: false \n";
  }
  if (isBusy())
  {
    result += "OrderID: " + std::to_string(getActiveOrderId()) + "\n";
  }
  else
  {
    result += "OrderID: None \n";
  }

  if (activeOrderId == NO_ORDER)
  {
    result += "timeLeft: None \n";
  }
  else
  {
    result += "timeLeft: " + std::to_string(getTimeLeft()) + "\n";
  }

  if (ordersLeft <= 0)
  {
    result += "OrdersLeft: None";
  }
  else
  {
    result += "OrdersLeft: " + std::to_string(ordersLeft);
  }
  return result;
}

LimitedCollectorVolunteer *LimitedCollectorVolunteer::clone() const
{
  return new LimitedCollectorVolunteer(*this);
}

//<--------------------------------------------------------------------------------------------->

DriverVolunteer ::DriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep) : Volunteer(id, name), maxDistance(maxDistance), distancePerStep(distancePerStep), distanceLeft(0) {}

int DriverVolunteer ::getDistanceLeft() const
{
  return distanceLeft;
}
int DriverVolunteer::getMaxDistance() const
{
  return maxDistance;
}

int DriverVolunteer::getDistancePerStep() const
{
  return distancePerStep;
}

bool DriverVolunteer::decreaseDistanceLeft()
{

  distanceLeft = distanceLeft - distancePerStep;

  if (distanceLeft <= 0)
  {
    distanceLeft = 0;
    return true;
  }

  return false;
}

bool DriverVolunteer ::hasOrdersLeft() const
{
  return true;
}

bool DriverVolunteer::canTakeOrder(const Order &order) const
{
  if (!isBusy() && order.getStatus() == OrderStatus::COLLECTING && order.getDistance() <= maxDistance)
  {
    return true;
  }
  else
  {
    return false;
  }
}

void DriverVolunteer::acceptOrder(const Order &order)
{
  if (canTakeOrder(order))
  {
    distanceLeft = order.getDistance();
    activeOrderId = order.getId();
  }
}

void DriverVolunteer::step()
{
  if (decreaseDistanceLeft())
  {
    completedOrderId = activeOrderId;
    activeOrderId = NO_ORDER;
  }
}
std::string DriverVolunteer::toString() const
{
  std::string result = "VolunteerID: " + std::to_string(getId()) + "\n";
  if (isBusy())
  {
    result += "isBusy: true \n";
  }
  else
  {
    result += "isBusy: false \n";
  }
  if (isBusy())
  {
    result += "OrderID: " + std::to_string(getActiveOrderId()) +"\n";
  }
  else
  {
    result += "OrderID: None\n";
  }

  if (distanceLeft <= 0)
  {
    result += "distanceLeft: None";
  }
  else
  {
    result += "distanceLeft: " + std::to_string(distanceLeft);
  }
  result += "\nOrdersLeft: No limit";

  return result;
}

DriverVolunteer *DriverVolunteer::clone() const
{
  return new DriverVolunteer(*this);
}

//<--------------------------------------------------------------------------------------------->

LimitedDriverVolunteer::LimitedDriverVolunteer(int id, const string &name, int maxDistance, int distancePerStep, int maxOrders) : DriverVolunteer(id, name, maxDistance, distancePerStep), maxOrders(maxOrders), ordersLeft(maxOrders) {}
int LimitedDriverVolunteer::getMaxOrders() const
{
  return maxOrders;
}

int LimitedDriverVolunteer::getNumOrdersLeft() const
{
  return ordersLeft;
}

bool LimitedDriverVolunteer::hasOrdersLeft() const
{
  return ordersLeft != 0;
}

bool LimitedDriverVolunteer ::canTakeOrder(const Order &order) const
{
  if (DriverVolunteer::canTakeOrder(order) & hasOrdersLeft())
  {
    return true;
  }
  return false;
}

void LimitedDriverVolunteer ::acceptOrder(const Order &order)
{
  if (canTakeOrder(order))
  {
    DriverVolunteer::acceptOrder(order);
    ordersLeft--;
  }
}

std::string LimitedDriverVolunteer::toString() const
{
  std::string result = "VolunteerID: " + std::to_string(getId());
  if (isBusy())
  {
    result += "\nisBusy: true \n";
  }
  else
  {
    result += "\nisBusy: false \n";
  }
  if (isBusy())
  {
    result += "OrderID: " + std::to_string(getActiveOrderId())+"\n";
  }
  else
  {
    result += "OrderID: None\n";
  }

  if (getDistanceLeft() <= 0)
  {
    result += "distanceLeft: None";
  }
  else
  {
    result += "distanceLeft: " + std::to_string(getDistanceLeft());
  }
  if (ordersLeft <= 0)
  {
    result += "\nOrdersLeft: None";
  }
  else
  {
    result += "\nOrdersLeft: " + std::to_string(ordersLeft);
  }

  return result;
}

LimitedDriverVolunteer *LimitedDriverVolunteer::clone() const
{
  return new LimitedDriverVolunteer(*this);
}